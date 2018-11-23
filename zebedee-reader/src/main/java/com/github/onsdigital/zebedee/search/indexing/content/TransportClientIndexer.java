package com.github.onsdigital.zebedee.search.indexing.content;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.search.configuration.SearchConfiguration;
import com.github.onsdigital.zebedee.search.fastText.FastTextClient;
import com.github.onsdigital.zebedee.search.fastText.FastTextHelper;
import com.github.onsdigital.zebedee.search.indexing.*;
import com.github.onsdigital.zebedee.util.URIUtils;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.elasticSearchLog;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;

public class TransportClientIndexer extends ZebedeeContentIndexer {

    private static final String SEARCH_ALIAS = SearchConfiguration.getSearchAlias();

    private final IndexClient indexClient;
    private final Lock lock;

    public TransportClientIndexer() {
        super();
        this.indexClient = IndexClient.getInstance();
        this.lock = new ReentrantLock();
    }

    @Override
    public void reindex() {
        // Acquire index lock
        if (this.lock.tryLock()) {
            try {
                // Lock in cluster
                this.lockGlobal();

                // Trigger reindex
                super.reindex();
            } finally {
                this.lock.unlock();
                this.indexClient.deleteGlobalLockDocument();
            }
        } else {
            throw new IndexInProgressException();
        }
    }

    /**
     * Index departments into the departments index
     */
    @Override
    public void indexDepartments() {
        // Reset the index
        if (this.indexClient.indexExists(Index.DEPARTMENTS.getIndex())) {
            this.indexClient.deleteIndex(Index.DEPARTMENTS.getIndex());
        }

        try {
            this.indexClient.createIndex(Index.DEPARTMENTS.getIndex(), ZebedeeContentIndexer.getDepartmentsSettings(),
                    Type.DEPARTMENTS.getType(), ZebedeeContentIndexer.getDepartmentsMapping());

            List<Department> departmentList = super.loadDepartments();

            // Index
            departmentList
                    .forEach(department -> this.indexClient.createDocument(Index.DEPARTMENTS.getIndex(),
                            Type.DEPARTMENTS.getType(), department.getCode(), department));

        } catch (IOException e) {
            String message = "Error while indexing departments";
            logError(e)
                    .addMessage(message)
                    .addParameter(IndexClient.Parameters.INDEX.getParameter(), Index.DEPARTMENTS.getIndex())
                    .log();
            throw new IndexingException(message, e);
        }
    }

    /**
     * Index content into the ons index
     */
    @Override
    public void indexOnsContent() {
        long start = System.currentTimeMillis();

        // Check if the index alias is available
        boolean aliasIsAvailable = this.indexClient.indexExists(SEARCH_ALIAS);

        // Get the old index from the current alias
        String oldIndex = this.indexClient.getIndexForAlias(SEARCH_ALIAS);

        // Generate new index name
        String newIndex = super.getOnsIndexName();

        // Process the possible scenarios
        if (aliasIsAvailable && oldIndex == null) {
            //In this case it is an index rather than an alias. This normally is not possible with index structure set up.
            //This is a transition code due to elastic search index structure change, making deployment to environments with old structure possible without down time
            this.indexClient.deleteIndex(SEARCH_ALIAS);
        }

        elasticSearchLog("Triggering re-indexing")
                .addParameter("index", newIndex)
                .log();
        try {
            // Create the new index
            this.indexClient.createIndex(newIndex, ZebedeeContentIndexer.getSettings(), Type.DEFAULT.getType(), ZebedeeContentIndexer.getDefaultMapping());

            List<Page> pages = super.loadPages();

            if (oldIndex == null) {
                this.indexClient.addIndexAlias(newIndex, SEARCH_ALIAS);
                this.indexPages(newIndex, pages);
            } else {
                this.indexPages(newIndex, pages);
                this.indexClient.swapIndexAlias(oldIndex, newIndex, SEARCH_ALIAS);
                this.indexClient.deleteIndex(oldIndex);
            }
            elasticSearchLog("Re-indexing completed")
                    .addParameter("totalTime(ms)", (System.currentTimeMillis() - start))
                    .log();
        } catch (IOException e) {
            logError(e)
                    .addMessage("Failed re-indexing content")
                    .addParameter(IndexClient.Parameters.INDEX.getParameter(), newIndex)
                    .log();
            throw new IndexingException("Failed re-indexing content", e);
        }
    }

    @Override
    public void indexByUri(String uri){
        Page page;
        List<Page> pages;

        //TODO: optimize resolving latest flag, only update elastic search for existing releases rather than reindexing
        //Load old releases as well to get latest flag re-calculated
        try {
            page = super.loadPageByUri(uri);
            if (page.getType().isPeriodic()) {
                pages = super.loadPages(URIUtils.removeLastSegment(uri));
            } else {
                pages = Collections.singletonList(page);
            }
        } catch (ZebedeeException | IOException e) {
            logError(e)
                    .addMessage("Content not found for re-indexing")
                    .addParameter("uri", uri)
                    .log();
            throw new IndexingException("Failed re-indexing content", e);
        }

        // Do the index
        this.indexPages(SEARCH_ALIAS, pages);
    }

    /**
     * Add pages to bulk processor for indexing
     * @param indexName
     * @param pages
     */
    private void indexPages(String indexName, List<Page> pages) {
        // Set embedding vectors
        if (FastTextHelper.Configuration.INDEX_EMBEDDING_VECTORS) {
            // Set embedding vectors
            try {
                pages = FastTextClient.getInstance().generateEmbeddingVectors(pages, 1000);
            } catch (Exception e) {
                logError(e)
                        .addMessage("Failed re-indexing content")
                        .addParameter(IndexClient.Parameters.INDEX.getParameter(), indexName)
                        .log();
                throw new IndexingException("Failed re-indexing", e);
            }
        }

        // Index the pages
        try (BulkProcessor bulkProcessor = this.indexClient.getBulkProcessor()) {
            pages.stream()
                    .map(Page::toSearchDocument)
                    .forEach(page -> {
                        IndexRequestBuilder requestBuilder = this.indexClient.createDocumentIndexRequest(
                                indexName,
                                page.getType().name(),
                                page.getUri().toString(),
                                page
                        );
                        bulkProcessor.add(requestBuilder.request());
                    });
        }
    }

    /**
     * Acquires a global index lock
     */
    private void lockGlobal() {
        IndexResponse lockResponse = this.indexClient.createGlobalLockDocument();
        if (!lockResponse.isCreated()) {
            IndexInProgressException indexInProgressException = new IndexInProgressException();
            logError(indexInProgressException).log();
            throw indexInProgressException;
        }
    }
}
