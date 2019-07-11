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
import static com.github.onsdigital.zebedee.logging.ReaderLogger.info;
import static com.github.onsdigital.zebedee.logging.ReaderLogger.warn;
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

                info().data("new_index", newIndex)
                        .log("reloading elastic search index");
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
                    info().data("old_index", oldIndex).log("deleting old elastic search index");
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
        info().data("index", indexName).log("triggering elastic search reindex");

        indexDocuments(indexName);

        info().data("index", indexName)
                .data("duration", (System.currentTimeMillis() - start))
                .log("elastic search reindex complete");
    }

    private void loadDepartments() throws IOException {

        if (isIndexAvailable(DEPARTMENTS_INDEX)) {
            searchUtils.deleteIndex(DEPARTMENTS_INDEX);
        }

        searchUtils.createIndex(DEPARTMENTS_INDEX, getDepartmentsSetting(), DEPARTMENT_TYPE, getDepartmentsMapping());

        info().log("elastic search: indexing departments");
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

        info().data("duration", (System.currentTimeMillis() - start))
                .log("elastic search: indexing departments complete");
    }

    private void processDepartment(String line) {
        if (isEmpty(line) || startsWith(line, "#")) {
            return; // skip comments
        }

        String[] split = line.split(" *=> *");
        if (split.length != 4) {
            warn().data("line", line).log("elastic search indexing departments: skipping invalid external line");
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
            info().data("uri", uri).log("elastic search: triggering reindex for uri");
            long start = System.currentTimeMillis();
            Page page = getPage(uri);
            if (page == null) {
                throw new NotFoundException("Content not found for re-indexing, uri: " + uri);
            }
            if (isPeriodic(page.getType())) {
                //TODO: optimize resolving latest flag, only update elastic search for existing releases rather than reindexing
                //Load old releases as well to get latest flag re-calculated
                index(getSearchAlias(), new FileScanner().scan(URIUtils.removeLastSegment(uri)));
            } else if (page.getType() == PageType.timeseries) {
                index(getSearchAlias(), new FileScanner().scan(uri));
            } else {
                indexSingleContent(getSearchAlias(), page);
            }
            long end = System.currentTimeMillis();
            info().data("uri", uri).data("duration", (start - end)).log("elastic search: reindex for uri complete");
        } catch (ZebedeeException e) {
            throw new IndexingException("Failed re-indexint content with uri: " + uri, e);
        } catch (NoSuchFileException e) {
            throw new IndexingException("Content not found for re-indexing, uri: " + uri);
        }
    }


    public void deleteContentIndex(String pageType, String uri) {
        info().data("uri", uri).log("elastic search: triggering delete index on publishing search index");
        long start = System.currentTimeMillis();
        searchUtils.deleteDocument(getSearchAlias(), pageType, uri);
        long end = System.currentTimeMillis();
        info().data("uri", uri).data("duration", (start - end)).log("elastic searchL delete index complete");
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

        info().data("settings", settingsBuilder.internalMap()).log("elastic search: index settings");
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
        info().data("mapping_source", mappingSource).log("elastic search: default mapping");
        return mappingSource;
    }

    private String getDepartmentsMapping() throws IOException {
        InputStream mappingSourceStream = Indexer.class.getResourceAsStream("/search/departments/departments-mapping.json");
        String mappingSource = IOUtils.toString(mappingSourceStream);
        info().data("mappingSource", mappingSource).log("elastic search: get departments mapping file");
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
                            long executionId, BulkRequest request) {
                        info().data("quantity", request.numberOfActions())
                                .log("elastic search bulk processor: bulk indexing  documents");
                    }

                    @Override
                    public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                        if (response.hasFailures()) {
                            BulkItemResponse[] items = response.getItems();
                            for (BulkItemResponse item : items) {
                                if (item.isFailed()) {
                                    info().data("uri", item.getFailure().getId())
                                            .data("detailed_message", item.getFailureMessage())
                                            .log("elastic search bulk processor: bulk indexing failure");
                                }
                            }
                        }
                    }

                    @Override
                    public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                        info().data("detailedMessagee", failure.getMessage())
                                .exception(failure)
                                .log("elastic search bulk processor: bulk indexing failure");
                    }
                })
                .setBulkActions(10000)
                .setBulkSize(new ByteSizeValue(100, ByteSizeUnit.MB))
                .setConcurrentRequests(4)
                .build();

        return bulkProcessor;
    }
}
