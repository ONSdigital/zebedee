package com.github.onsdigital.zebedee.search.indexing.content;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.search.configuration.SearchConfiguration;
import com.github.onsdigital.zebedee.search.fastText.FastTextClient;
import com.github.onsdigital.zebedee.search.fastText.FastTextHelper;
import com.github.onsdigital.zebedee.search.indexing.Department;
import com.github.onsdigital.zebedee.search.indexing.Index;
import com.github.onsdigital.zebedee.search.indexing.IndexClient;
import com.github.onsdigital.zebedee.search.indexing.IndexingException;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequestBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.elasticSearchLog;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;

public class NodeClientIndexer extends ZebedeeContentIndexer {

    private final IndexClient indexClient;

    public NodeClientIndexer() {
        super();
        this.indexClient = IndexClient.getInstance();
    }

    @Override
    public void indexDepartments() {
        // Reset the index
        if (this.indexClient.indexExists(Index.DEPARTMENTS.getIndex())) {
            this.indexClient.deleteIndex(Index.DEPARTMENTS.getIndex());
        }

        try {
            this.indexClient.createIndex(Index.DEPARTMENTS.getIndex(), ZebedeeContentIndexer.getDepartmentsSettings(), ZebedeeContentIndexer.getDepartmentsMapping());
        } catch (IOException e) {
            logError(e)
                    .addMessage("Failed re-indexing content")
                    .addParameter(IndexClient.Parameters.INDEX.getParameter(), Index.DEPARTMENTS.getIndex())
                    .log();
            throw new IndexingException("Failed re-indexing content", e);
        }

        // Load the departments
        try {
            List<Department> departmentList = super.loadDepartments();

            // Index
            departmentList
                    .forEach(department -> this.indexClient.createDocument(Index.DEPARTMENTS.getIndex(),
                            DEPARTMENT_TYPE, department.getCode(), department));

        } catch (IOException e) {
            // TODO - better logging
            logError(e)
                    .addMessage("Error while indexing departments")
                    .addParameter(IndexClient.Parameters.INDEX.getParameter(), Index.DEPARTMENTS.getIndex())
                    .log();
        }
    }

    @Override
    public void indexOnsContent() {
        long start = System.currentTimeMillis();

        // Get the search alias
        String searchAlias = SearchConfiguration.getSearchAlias();

        // Check if the index alias is available
        boolean aliasIsAvailable = this.indexClient.indexExists(searchAlias);

        // Get the old index from the current alias
        String oldIndex = this.indexClient.getIndexForAlias(searchAlias);

        // Generate new index name
        String newIndex = this.generateIndexName();

        elasticSearchLog("Triggering re-indexing")
                .addParameter("index", newIndex)
                .log();
        try {
            // Create the new index
            this.indexClient.createIndex(newIndex, ZebedeeContentIndexer.getSettings(), ZebedeeContentIndexer.getDefaultMapping());

            // Process the possible scenarious
            if (aliasIsAvailable && oldIndex == null) {
                //In this case it is an index rather than an alias. This normally is not possible with index structure set up.
                //This is a transition code due to elastic search index structure change, making deployment to environments with old structure possible without down time
                this.indexClient.deleteIndex(searchAlias);
            }

            if (oldIndex == null) {
                this.indexClient.addIndexAlias(newIndex, searchAlias);
                this.indexContent(newIndex);
            } else {
                this.indexContent(newIndex);
                this.indexClient.swapIndexAlias(oldIndex, newIndex, searchAlias);
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
    public void indexByUri(URI uri) {

    }

    private String generateIndexName() {
        return SearchConfiguration.getSearchAlias() + System.currentTimeMillis();
    }

    private void indexContent(String indexName) throws IOException {
        List<Page> pages = super.loadPages();

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
}
