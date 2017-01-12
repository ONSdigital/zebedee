package com.github.onsdigital.zebedee.search.indexing;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.onsdigital.zebedee.content.util.ContentUtil.serialise;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.elasticSearchLog;
import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.getSearchAlias;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.startsWith;

public class Indexer {
    private final static String DEPARTMENTS_INDEX = "departments";
    private final static String DEPARTMENT_TYPE = "departments";
    private final static String DEPARTMENTS_PATH = "/search/departments/departments.txt";
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

    public static void main(String[] args) {
        try {
            Indexer.getInstance().reload();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes search index and aliases, it should be run on application start.
     */
    public void reload() throws IOException {
        if (LOCK.tryLock()) {
            try {
                lockGlobal();//lock in cluster
                String searchAlias = getSearchAlias();
                boolean aliasAvailable = isIndexAvailable(searchAlias);
                String oldIndex = searchUtils.getAliasIndex(searchAlias);
                String newIndex = generateIndexName();
                elasticSearchLog("Creating index").addParameter("newIndex", newIndex).log();
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
                    elasticSearchLog("Deleting old index").addParameter("oldIndex", oldIndex).log();
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

    public boolean isIndexAvailable(String indexName) {
        return searchUtils.isIndexAvailable(indexName);
    }

    private void doLoad(String indexName) throws IOException {
        loadDepartments();
        loadContent(indexName);
    }

    private void loadContent(String indexName) throws IOException {
        long start = System.currentTimeMillis();
        elasticSearchLog("Triggering re-indexing")
                .addParameter("index", indexName)
                .log();
        indexDocuments(indexName);
        elasticSearchLog("Re-indexing completed")
                .addParameter("totalTime(ms)", (System.currentTimeMillis() - start))
                .log();
    }

    private void loadDepartments() throws IOException {

        if (isIndexAvailable(DEPARTMENTS_INDEX)) {
            searchUtils.deleteIndex(DEPARTMENTS_INDEX);
        }

        searchUtils.createIndex(DEPARTMENTS_INDEX, getDepartmentsSetting(), DEPARTMENT_TYPE, getDepartmentsMapping());

        elasticSearchLog("Indexing departments").log();
        long start = System.currentTimeMillis();
        try (
                InputStream resourceStream = SearchBoostTermsResolver.class.getResourceAsStream(DEPARTMENTS_PATH);
                InputStreamReader inputStreamReader = new InputStreamReader(resourceStream);
                BufferedReader br = new BufferedReader(inputStreamReader)
        ) {
            for (String line; (line = br.readLine()) != null; ) {
                processDepartment(line);
            }
        }
        elasticSearchLog("Indexing departments complete")
                .addParameter("totalTime(ms)", (System.currentTimeMillis() - start))
                .log();
    }

    private void processDepartment(String line) {
        if (isEmpty(line) || startsWith(line, "#")) {
            return; // skip comments
        }

        String[] split = line.split(" *=> *");
        if (split.length != 4) {
            elasticSearchLog("Skipping invalid external department").addParameter("line", line).log();
            return;
        }
        String[] terms = split[3].split(" *, *");
        if (terms == null || terms.length == 0) {
            return;
        }

        Department department = new Department(split[0], split[1], split[2], terms);
        searchUtils.createDocument(DEPARTMENTS_INDEX, DEPARTMENT_TYPE, split[0], ContentUtil.serialise(department));
    }

    /**
     * Reads content with given uri and indexes for search
     *
     * @param uri
     */

    public void reloadContent(String uri) throws IOException {
        try {
            elasticSearchLog("Triggering reindex").addParameter("uri", uri).log();
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
            elasticSearchLog("Reindexing complete")
                    .addParameter("uri", uri)
                    .addParameter("totalTime(ms)", (start - end))
                    .log();
        } catch (ZebedeeException e) {
            throw new IndexingException("Failed re-indexint content with uri: " + uri, e);
        } catch (NoSuchFileException e) {
            throw new IndexingException("Content not found for re-indexing, uri: " + uri);
        }
    }


    public void deleteContentIndex(String pageType, String uri) {
        elasticSearchLog("Triggering delete index on publishing search index").addParameter("uri", uri).log();
        long start = System.currentTimeMillis();
        searchUtils.deleteDocument(getSearchAlias(), pageType, uri);
        long end = System.currentTimeMillis();
        elasticSearchLog("Delete index complete")
                .addParameter("uri", uri)
                .addParameter("totalTime(ms)", (start - end))
                .log();
    }

    private String generateIndexName() {
        return getSearchAlias() + System.currentTimeMillis();
    }

    /**
     * Resolves search terms for a single document
     */
    private List<String> resolveSearchTerms(String uri) throws IOException {
        if (uri == null) {
            return null;
        }

        SearchBoostTermsResolver resolver = SearchBoostTermsResolver.getSearchTermResolver();
        List<String> terms = new ArrayList<>();
        addTerms(terms, resolver.getTerms(uri));

        String[] segments = uri.split("/");
        for (String segment : segments) {
            String documentUri = "/" + segment;
            addTerms(terms, resolver.getTermsForPrefix(documentUri));
        }
        return terms;
    }

    private void addTerms(List<String> termsList, List<String> terms) {
        if (terms == null) {
            return;
        }
        termsList.addAll(terms);
    }

    private void indexDocuments(String indexName) throws IOException {
        index(indexName, new FileScanner().scan());
    }

    /**
     * Recursively indexes contents and their child contents
     *
     * @param indexName
     * @param documents
     * @throws IOException
     */
    private void index(String indexName, List<Document> documents) throws IOException {
        try (BulkProcessor bulkProcessor = getBulkProcessor()) {
            for (Document document : documents) {
                try {
                    IndexRequestBuilder indexRequestBuilder = prepareIndexRequest(indexName, document);
                    if (indexRequestBuilder == null) {
                        continue;
                    }
                    bulkProcessor.add(indexRequestBuilder.request());
                } catch (Exception e) {
                    System.err.println("!!!!!!!!!Failed preparing index for " + document.getUri() + " skipping...");
                    e.printStackTrace();
                }
            }
        }
    }

    private Page getPage(String uri) throws ZebedeeException, IOException {
        return zebedeeReader.getPublishedContent(uri);
    }

    private IndexRequestBuilder prepareIndexRequest(String indexName, Document document) throws ZebedeeException, IOException {
        Page page = getPage(document.getUri());
        if (page != null && page.getType() != null) {
            IndexRequestBuilder indexRequestBuilder = searchUtils.prepareIndex(indexName, page.getType().name(), page.getUri().toString());
            indexRequestBuilder.setSource(serialise(toSearchDocument(page, document.getSearchTerms())));
            return indexRequestBuilder;
        }
        return null;
    }

    private void indexSingleContent(String indexName, Page page) throws IOException {
        List<String> terms = resolveSearchTerms(page.getUri().toString());
        searchUtils.createDocument(indexName, page.getType().toString(), page.getUri().toString(), serialise(toSearchDocument(page, terms)));
    }

    private SearchDocument toSearchDocument(Page page, List<String> searchTerms) {
        SearchDocument indexDocument = new SearchDocument();
        indexDocument.setUri(page.getUri());
        indexDocument.setDescription(page.getDescription());
        indexDocument.setTopics(getTopics(page.getTopics()));
        indexDocument.setType(page.getType());
        indexDocument.setSearchBoost(searchTerms);
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
        elasticSearchLog("Index settings").addParameter("settings", settingsBuilder.internalMap());
        return settingsBuilder.build();
    }

    private Settings getDepartmentsSetting() {
        Settings.Builder settingsBuilder = Settings.builder().
                loadFromStream("departments-index-config.yml", Indexer.class.getResourceAsStream("/search/departments/departments-index-config.yml"));
        return settingsBuilder.build();
    }

    private String getDefaultMapping() throws IOException {
        InputStream mappingSourceStream = Indexer.class.getResourceAsStream("/search/default-mapping.json");
        String mappingSource = IOUtils.toString(mappingSourceStream);
        elasticSearchLog("defaultMapping").addParameter("mappingSource", mappingSource).log();
        return mappingSource;
    }

    private String getDepartmentsMapping() throws IOException {
        InputStream mappingSourceStream = Indexer.class.getResourceAsStream("/search/departments/departments-mapping.json");
        String mappingSource = IOUtils.toString(mappingSourceStream);
        elasticSearchLog("departmentsMapping").addParameter("mappingSource", mappingSource).log();
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
                    public void beforeBulk(
                            long executionId,
                            BulkRequest request
                    ) {
                        elasticSearchLog("Bulk Indexing documents").addParameter("quantity", request.numberOfActions()).log();
                    }

                    @Override
                    public void afterBulk(
                            long executionId,
                            BulkRequest request,
                            BulkResponse response
                    ) {
                        if (response.hasFailures()) {
                            BulkItemResponse[] items = response.getItems();
                            for (BulkItemResponse item : items) {
                                if (item.isFailed()) {
                                    elasticSearchLog("Indexing failure")
                                            .addParameter("uri", item.getFailure().getId())
                                            .addParameter("detailMessage", item.getFailureMessage())
                                            .log();
                                }
                            }
                        }
                    }

                    @Override
                    public void afterBulk(
                            long executionId,
                            BulkRequest request,
                            Throwable failure
                    ) {
                        elasticSearchLog("Bulk index failure")
                                .addParameter("detailedMessagee", failure.getMessage())
                                .log();
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
