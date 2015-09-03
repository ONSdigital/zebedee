package com.github.onsdigital.zebedee.search.indexing;

import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import com.github.onsdigital.zebedee.search.model.SearchDocument;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.onsdigital.zebedee.content.util.ContentUtil.serialise;
import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.getElasticSearchIndexAlias;

public class Indexer {
    private static Indexer instance = new Indexer();

    private final Lock LOCK = new ReentrantLock();
    private final String ROOT_URI = "/";

    private ElasticSearchWriter elasticSearchWriter = new ElasticSearchWriter(ElasticSearchClient.getClient());
    private ZebedeeReader zebedeeReader = new ZebedeeReader();
    private String currentIndex;

    private Indexer() {
    }


    /**
     * Loads documents to a new index, switches default index alias to new index and deletes old index if available  to ensure zero down time.
     *
     * @throws IOException
     */
    public void loadIndex() throws IOException {
        if (LOCK.tryLock()) {
            try {
                reload();
            } finally {
                LOCK.unlock();
            }
        } else {
            throw new IndexInProgressException();
        }
    }

    public void reload() throws IOException {
        String newIndex = generateIndexName();
        elasticSearchWriter.createIndex(newIndex);
        indexDocuments(newIndex);
        elasticSearchWriter.swapIndex(currentIndex, newIndex, getElasticSearchIndexAlias());
        if (currentIndex != null) {
            elasticSearchWriter.deleteIndex(currentIndex);
        }
        currentIndex = newIndex;
    }

    /**
     *
     *Reads content with given uri and indexes for search
     *
     * @param uri
     */
    public void reloadContent(URI uri) throws IOException {
        try {
            loadAndIndex(getElasticSearchIndexAlias(), uri);
        } catch (ZebedeeException e) {
            throw new IndexingException("Failed indexing: ", e);
        }
    }


    private String generateIndexName() {
        return getElasticSearchIndexAlias() + System.currentTimeMillis();
    }


    private void indexDocuments(String indexName) throws IOException {

        try {
            Map<URI, ContentNode> contents = zebedeeReader.getPublishedContentChildren(ROOT_URI);
            index(indexName, contents);
        } catch (ZebedeeException e) {
            throw new IndexingException("Failed indexing: ", e);
        }
    }

    /**
     * Recursively indexes contents and their child contents
     *
     * @param indexName
     * @param contents
     * @throws IOException
     */
    private void index(String indexName, Map<URI, ContentNode> contents) throws IOException, ZebedeeException {
        for (ContentNode content : contents.values()) {
            if (content.getType() != null) { // skip folders with no data file and unknown contents
                loadAndIndex(indexName, content.getUri());
            }
            index(indexName, zebedeeReader.getPublishedContentChildren(content.getUri().toString()));//index children
        }
    }

    private void loadAndIndex(String indexName, URI uri) throws ZebedeeException, IOException {
        //TODO:Change zebedee reader for parametrized object mapping
        Page content = zebedeeReader.getPublishedContent(uri.toString());
        elasticSearchWriter.createDocument(indexName, content.getType().name(), content.getUri().toString(), serialise(toSearchDocument(content)));
    }


    private SearchDocument toSearchDocument(Page page) {
        SearchDocument indexDocument = new SearchDocument();
        indexDocument.setUri(page.getUri());
        indexDocument.setDescription(page.getDescription());
        indexDocument.setType(page.getType());
        return indexDocument;
    }

    public static Indexer getInstance() {
        return instance;
    }


    //    // Mapping for field properties. To decide which field will be indexed
//    private static XContentBuilder getMappingProperties(String type) throws IOException {
//
//        XContentBuilder builder = jsonBuilder().startObject().startObject(type).startObject("properties");
//        try {
//            builder.startObject("releaseDate").field("type", "date").field("index", "analyzed").endObject();
//            builder.startObject("summary").field("type", "string").field("index", "no").endObject();
//            builder.startObject("title").field("type", "string").field("index", "analyzed").endObject();
//            builder.startObject("tags").field("type", "string").field("index", "analyzed").endObject();
//            builder.startObject("edition").field("type", "string").field("index", "analyzed").endObject();
//
//            //builder.startObject("uri").field("type", "string").field("index", "analyzed").endObject();
//
//            builder.startObject("uri")
//                    .field("type", "multi_field")
//                    .startObject("fields")
//                    .startObject("uri")
//                    .field("type", "string")
//                    .field("index", "analyzed")
//                    .endObject()
//                    .startObject("uri_segment")
//                    .field("type", "string")
//                    .field("index", "analyzed")
//                    .field("index_analyzer", "whitespace")
//                    .field("search_analyzer", "whitespace")
//                    .endObject()
//                    .endObject()
//                    .endObject();
//
//            builder.endObject().endObject().endObject();
//            return builder;
//        } finally {
//            if (builder != null) {
//                builder.close();
//            }
//        }
//    }


//    private static Map<String, String> buildSettings() throws IOException {
//        ImmutableSettings.Builder settingsBuilder = ImmutableSettings.settingsBuilder();
//
//        List<String> synonymList = getSynonyms(settingsBuilder);
//        getSettingsBuilder(settingsBuilder, synonymList);
//
//        return settingsBuilder.build().getAsMap();
//    }

//    private static void getSettingsBuilder(ImmutableSettings.Builder settingsBuilder, List<String> synonymList) {
//        String[] synonyms = new String[synonymList.size()];
//        synonymList.toArray(synonyms);
//
//        settingsBuilder.putArray("analysis.filter.ons_synonym_filter.synonyms", synonyms);
//
//        Map<String, String> settings = new HashMap<>();
//        // default analyzer
//        settings.put("analysis.analyzer.default_index.tokenizer", "ons_search_tokenizer");
//        settings.put("analysis.analyzer.default_index.filter", "lowercase");
//        settings.put("analysis.analyzer.ons_synonyms.tokenizer", "standard");
//        settings.put("analysis.filter.ons_synonym_filter.type", "synonym");
//
//        // edgeNGram tokenizer
//        settings.put("analysis.tokenizer.ons_search_tokenizer.type", "edgeNGram");
//        settings.put("analysis.tokenizer.ons_search_tokenizer.max_gram", "15");
//        settings.put("analysis.tokenizer.ons_search_tokenizer.min_gram", "2");
//        String[] tokenChars = {"letter", "digit"};
//        settingsBuilder.putArray("analysis.tokenizer.ons_search_tokenizer.token_chars", tokenChars);
//
//        settingsBuilder.put(settings);
//    }

//    private static List<String> getSynonyms(ImmutableSettings.Builder settingsBuilder) throws IOException {
//        String[] filters = {"lowercase", "ons_synonym_filter"};
//        settingsBuilder.putArray("analysis.analyzer.ons_synonyms.filter", filters);
//
//        // java 7 try-with-resources automatically closes streams after use
//        try (InputStream inputStream = Indexer.class.getResourceAsStream("/synonym.txt"); BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
//
//            List<String> synonymList = new ArrayList<String>();
//            String contents = null;
//            while ((contents = reader.readLine()) != null) {
//                if (!contents.startsWith("#")) {
//                    synonymList.add(contents);
//                }
//            }
//            return synonymList;
//        }
//    }

}
