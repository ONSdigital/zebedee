package com.github.onsdigital.zebedee.search.indexing;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.Link;
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
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.onsdigital.zebedee.content.util.ContentUtil.serialise;
import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.getSearchAlias;

public class Indexer {
    private static Indexer instance = new Indexer();

    private final Lock LOCK = new ReentrantLock();

    private final Client client = ElasticSearchClient.getClient();
    private ElasticSearchUtils searchUtils = new ElasticSearchUtils(client);
    private ZebedeeReader zebedeeReader = new ZebedeeReader();

    private Indexer() {
    }

    public static Indexer getInstance() {
        return instance;
    }

    /**
     * Initializes search index and aliases, it should be run on application start.
     */
    public void reload() throws IOException {
        if (LOCK.tryLock()) {
            try {
                lockGlobal();//lock in cluster
                String searchAlias = getSearchAlias();
                boolean aliasAvailable = searchUtils.isIndexAvailable(searchAlias);
                String oldIndex = searchUtils.getAliasIndex(searchAlias);
                String newIndex = generateIndexName();
                System.out.println("Creating index:" + newIndex);
                searchUtils.createIndex(newIndex, getSettings(), getDefaultMapping());

                if (aliasAvailable && oldIndex == null) {
                    //In this case it is an index rather than an alias. This normally is not possible with index structure set up.
                    //This is a transition code due to elastic search index structure change, making deployment to environments with old structure possible without down time
                    searchUtils.deleteIndex(searchAlias);
                    searchUtils.addAlias(newIndex, searchAlias);
                    doLoad(newIndex);
                } else if (oldIndex == null) {
                    searchUtils.addAlias(newIndex, searchAlias);
                    doLoad(newIndex);
                } else {
                    doLoad(newIndex);
                    searchUtils.swapIndex(oldIndex, newIndex, searchAlias);
                    System.out.println("Deleting old index:" + oldIndex);
                    searchUtils.deleteIndex(oldIndex);
                }
            } finally {
                LOCK.unlock();
                unlockGlobal();
            }
        } else {
            throw new IndexInProgressException();
        }
    }

    private void doLoad(String indexName) throws IOException {
        long start = System.currentTimeMillis();
        System.out.println("Triggering re-indexing on index:" + indexName);
        indexDocuments(indexName);
        long end = System.currentTimeMillis();
        System.out.println("Elasticsearch: indexing complete in " + (end - start) + " ms");
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
                //TODO: optimize resolving latest flag, only update elastic search for existing releases rather than reindexing
                //Load old releases as well to get latest flag re-calculated
                index(getSearchAlias(), new FileScanner().scan(URIUtils.removeLastSegment(uri)));
            } else {
                indexSingleContent(getSearchAlias(), page);
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
        return getSearchAlias() + System.currentTimeMillis();
    }


    private void indexDocuments(String indexName) throws IOException {
        index(indexName, new FileScanner().scan());
    }

    /**
     * Recursively indexes contents and their child contents
     *
     * @param indexName
     * @param fileNames
     * @throws IOException
     */
    private void index(String indexName, List<String> fileNames) throws IOException {
        try (BulkProcessor bulkProcessor = getBulkProcessor()) {
            for (String path : fileNames) {
                try {
                    IndexRequestBuilder indexRequestBuilder = prepareIndexRequest(indexName, path);
                    if (indexRequestBuilder == null) {
                        continue;
                    }
                    bulkProcessor.add(indexRequestBuilder.request());
                } catch (Exception e) {
                    System.err.println("!!!!!!!!!Failed preparing index for " + path + " skipping...");
                    e.printStackTrace();
                }
            }
        }
    }


    private Page getPage(String uri) throws ZebedeeException, IOException {
        return zebedeeReader.getPublishedContent(uri);
    }

    private IndexRequestBuilder prepareIndexRequest(String indexName, String uri) throws ZebedeeException, IOException {
        Page page = getPage(uri);
        if (page != null && page.getType() != null) {
            IndexRequestBuilder indexRequestBuilder = searchUtils.prepareIndex(indexName, page.getType().name(), page.getUri().toString());
            indexRequestBuilder.setSource(serialise(toSearchDocument(page)));
            return indexRequestBuilder;
        }
        return null;
    }

    private void indexSingleContent(String indexName, Page page) {
        searchUtils.createDocument(indexName, page.getType().toString(), page.getUri().toString(), serialise(toSearchDocument(page)));
    }


    private SearchDocument toSearchDocument(Page page) {
        SearchDocument indexDocument = new SearchDocument();
        indexDocument.setUri(page.getUri());
        indexDocument.setDescription(page.getDescription());
        indexDocument.setTopics(getTopics(page.getTopics()));
        indexDocument.setType(page.getType());
        return indexDocument;
    }

    private ArrayList<URI> getTopics(List<Link> topics) {
        if (topics == null) {
            return null;
        }
        ArrayList<URI> uriList = new ArrayList<>();
        for (Link topic : topics) {
            uriList.add(topic.getUri());
        }

        return uriList;
    }

    private Settings getSettings() throws IOException {
        Settings.Builder settingsBuilder = Settings.builder().
                loadFromStream("index-config.yml", Indexer.class.getResourceAsStream("/search/index-config.yml"));
        System.out.println("Index settings:\n" + settingsBuilder.internalMap());
        return settingsBuilder.build();
    }


    private String getDefaultMapping() throws IOException {
        InputStream mappingSourceStream = Indexer.class.getResourceAsStream("/search/default-mapping.json");
        String mappingSource = IOUtils.toString(mappingSourceStream);
        System.out.println(mappingSource);
        return mappingSource;

    }

    //acquires global lock
    private void lockGlobal() {
        IndexResponse lockResponse = searchUtils.createDocument("fs", "lock", "global", "{}");
        if (!lockResponse.isCreated()) {
            throw new IndexInProgressException();
        }
    }


    private void unlockGlobal() {
        searchUtils.deleteDocument("fs", "lock", "global");
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

    public static void main(String[] args) {
        try {
            Indexer.getInstance().reload();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
