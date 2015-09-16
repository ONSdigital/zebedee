package com.github.onsdigital.zebedee.search.indexing;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import com.github.onsdigital.zebedee.search.model.SearchDocument;
import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;

import java.io.*;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.onsdigital.zebedee.content.util.ContentUtil.serialise;
import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.getElasticSearchIndexAlias;

public class Indexer {
    private static Indexer instance = new Indexer();

    private final Lock LOCK = new ReentrantLock();

    private final Client client = ElasticSearchClient.getClient();
    private ElasticSearchWriter elasticSearchWriter = new ElasticSearchWriter(client);
    private ZebedeeReader zebedeeReader = new ZebedeeReader();
    private String currentIndex;

    private Indexer() {
    }

    public static Indexer getInstance() {
        return instance;
    }

    /**
     * Loads documents to a new index, switches default index alias to new index and deletes old index if available  to ensure zero down time.
     *
     * @throws IOException
     */
    public void reload() throws IOException {
        if (LOCK.tryLock()) {
            try {
                long start = System.currentTimeMillis();
                System.out.println("Triggering reindex");
                doLoad();
                long end = System.currentTimeMillis();
                System.out.println("Elasticsearch: indexing complete in" + (start - end) + " ms");
            } finally {
                LOCK.unlock();
            }
        } else {
            throw new IndexInProgressException();
        }
    }

    private void doLoad() throws IOException {
        String newIndex = generateIndexName();
        elasticSearchWriter.createIndex(newIndex, getSettings(), getDefaultMapping());
        if (currentIndex == null) {
            //if first load add alias right away, do not wait documents get indexed
            elasticSearchWriter.addAlias(newIndex, getElasticSearchIndexAlias());
            indexDocuments(newIndex);
        } else {
            indexDocuments(newIndex);
            elasticSearchWriter.swapIndex(currentIndex, newIndex, getElasticSearchIndexAlias());
            elasticSearchWriter.deleteIndex(currentIndex);
        }
        currentIndex = newIndex;
    }

    /**
     * Reads content with given uri and indexes for search
     *
     * @param uri
     */
    public void reloadContent(String uri) throws IOException {
        try {
            System.out.println("Triggering reindex for content, uri:" + uri);
            long start = System.currentTimeMillis();
            Page page = getPage(uri);
            if (page == null) {
                throw new NotFoundException("Content not found for re-indexing, uri: " + uri);
            }
            if (isPeriodic(page.getType())) {
                //TODO: optimize resolving lastest flag, only update elastic search for existing releases rather than reindexing
                //Load old releases as well to get latest flag re-calculated
                index(currentIndex, new FileScanner().scan(URIUtils.removeLastSegment(uri)));
            } else {
                indexSingleContent(currentIndex, page);
            }
            long end = System.currentTimeMillis();
            System.out.println("Elasticsearch: indexing complete for uri " + uri + " in " + (start - end) + " ms");
        } catch (ZebedeeException e) {
            throw new IndexingException("Failed re-indexint content with uri: " + uri, e);
        } catch (NoSuchFileException e) {
            throw new IndexingException("Content not found for re-indexing, uri: " + uri);
        }
    }

    private String generateIndexName() {
        return getElasticSearchIndexAlias() + System.currentTimeMillis();
    }


    private void indexDocuments(String indexName) throws IOException {

        try {
            index(indexName, new FileScanner().scan());
        } catch (ZebedeeException e) {
            throw new IndexingException("Failed indexing: ", e);
        }
    }

    /**
     * Recursively indexes contents and their child contents
     *
     * @param indexName
     * @param fileNames
     * @throws IOException
     */
    private void index(String indexName, List<String> fileNames) throws IOException, ZebedeeException {
        try (BulkProcessor bulkProcessor = getBulkProcessor()) {
            for (String path : fileNames) {
                IndexRequestBuilder indexRequestBuilder = prepareIndexRequest(indexName, path);
                bulkProcessor.add(indexRequestBuilder.request());
            }
        }
    }


    private Page getPage(String uri) throws ZebedeeException, IOException {
        return zebedeeReader.getPublishedContent(uri);
    }

    private IndexRequestBuilder prepareIndexRequest(String indexName, String uri) throws ZebedeeException, IOException {
        Page page = getPage(uri);
        if (page != null && page.getType() != null) {
            IndexRequestBuilder indexRequestBuilder = elasticSearchWriter.prepareIndex(indexName, page.getType().name(), page.getUri().toString());
            indexRequestBuilder.setSource(serialise(toSearchDocument(page)));
            return indexRequestBuilder;
        }
        return null;
    }

    private void indexSingleContent(String indexName, Page page) {
        elasticSearchWriter.createDocument(indexName, page.getType().toString(), page.getUri().toString(), serialise(toSearchDocument(page)));
    }


    private SearchDocument toSearchDocument(Page page) {
        SearchDocument indexDocument = new SearchDocument();
        indexDocument.setUri(page.getUri());
        indexDocument.setDescription(page.getDescription());
        indexDocument.setType(page.getType());
        return indexDocument;
    }

    private Settings getSettings() throws IOException {
        ImmutableSettings.Builder settingsBuilder = ImmutableSettings.settingsBuilder().loadFromUrl(Indexer.class.getResource("/search/index-config.yml"));
        System.out.println("Index settings:\n" + settingsBuilder.internalMap());
        return settingsBuilder.build();
    }


    private String getDefaultMapping() throws IOException {
        InputStream mappingSourceStream = Indexer.class.getResourceAsStream("/search/default-mapping.json");
        String mappingSource = IOUtils.toString(mappingSourceStream);
        System.out.println(mappingSource);
        return mappingSource;

    }


    private boolean isPeriodic(PageType type) {
        switch (type) {
            case bulletin:
            case article:
            case compendium_landing_page:
                return true;
            default:
                return false;
        }
    }

    private BulkProcessor getBulkProcessor() {
        BulkProcessor bulkProcessor = BulkProcessor.builder(
                client,
                new BulkProcessor.Listener() {
                    @Override
                    public void beforeBulk(long executionId,
                                           BulkRequest request) {
                        System.out.println("Builk Indexing " + request.numberOfActions() + " documents");
                    }

                    @Override
                    public void afterBulk(long executionId,
                                          BulkRequest request,
                                          BulkResponse response) {
                        if (response.hasFailures()) {
                            BulkItemResponse[] items = response.getItems();
                            for (BulkItemResponse item : items) {
                                if (item.isFailed()) {
                                    System.err.println("!!!!!!!!Failed indexing: [uri:" + item.getFailure().getId() + " error:" + item.getFailureMessage() + "]");
                                }
                            }
                        }
                    }

                    @Override
                    public void afterBulk(long executionId,
                                          BulkRequest request,
                                          Throwable failure) {
                        System.err.println("Failed executing bulk index :" + failure.getMessage());
                        failure.printStackTrace();
                    }
                })
                .setBulkActions(10000)
                .setBulkSize(new ByteSizeValue(100, ByteSizeUnit.MB))
                .setConcurrentRequests(4)
                .build();

        return bulkProcessor;
    }
}
