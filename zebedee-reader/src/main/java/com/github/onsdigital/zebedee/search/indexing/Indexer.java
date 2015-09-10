package com.github.onsdigital.zebedee.search.indexing;

import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import com.github.onsdigital.zebedee.search.model.SearchDocument;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.onsdigital.zebedee.content.util.ContentUtil.serialise;
import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.getElasticSearchIndexAlias;
import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.isDisableReindex;

public class Indexer {
    private static Indexer instance = new Indexer();

    private final Lock LOCK = new ReentrantLock();
    private final String ROOT_URI = "/";

    private ElasticSearchWriter elasticSearchWriter = new ElasticSearchWriter(ElasticSearchClient.getClient());
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
    public void reloadIndex() throws IOException {
        if (isDisableReindex()) {
            System.out.println("Skipping reindexing due to configuration");
            return;
        }

        if (LOCK.tryLock()) {
            try {
                doLoad();
            } finally {
                LOCK.unlock();
            }
        } else {
            throw new IndexInProgressException();
        }
    }

    private void doLoad() throws IOException {
        String newIndex = generateIndexName();
        elasticSearchWriter.createIndex(newIndex, getSettings(),getDefaultMapping());
        indexDocuments(newIndex);
        elasticSearchWriter.swapIndex(currentIndex, newIndex, getElasticSearchIndexAlias());
        if (currentIndex != null) {
            elasticSearchWriter.deleteIndex(currentIndex);
        }
        currentIndex = newIndex;
    }

    /**
     * Reads content with given uri and indexes for search
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


    // Mapping for field properties. To decide which field will be indexed
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

}
