package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollection;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollectionSearchResult;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Represents the store of published collections for adding to and searching.
 */
public class PublishedCollections {

    private static final String index = "publishedcollections";
    private static final String mapping = "collection";
    private static final String type = "collection";

    public final Path path;
    private final Zebedee zebedee;

    private boolean initialised = false;

    public PublishedCollections(Path path, Zebedee zebedee) {
        this.path = path;
        this.zebedee = zebedee;
    }

    private static XContentBuilder getMappingProperties(String type) throws IOException {

        XContentBuilder builder = jsonBuilder().startObject().startObject(type).startObject("properties");
        try {
            builder.startObject("id").field("type", "string").field("index", "analyzed").endObject();
            builder.startObject("name").field("type", "string").field("index", "analyzed").endObject();
            builder.startObject("type").field("type", "string").field("index", "analyzed").endObject();
            builder.startObject("publishDate").field("type", "date").field("index", "analyzed").endObject();
            builder.endObject().endObject().endObject();
            return builder;
        } finally {
            if (builder != null) {
                builder.close();
            }
        }
    }

    // todo: Currently 'lazy loading' the index, need a hook on the end of initialising elastic search
    private void tryInit(Client client) throws IOException {
        if (!initialised) {
            init(client);
        }
    }

    public synchronized void init(Client client) throws IOException {
        if (!initialised) {
            boolean exists = client.admin().indices().prepareExists(index).execute().actionGet().isExists();

            if (!exists) {
                logInfo("Creating search index for published collections.").log();
                CreateIndexRequestBuilder indexBuilder = client.admin().indices()
                        .prepareCreate(index);

                logInfo("Adding mapping for published collections index").log();
                //indexBuilder.addMapping(mapping, getMappingProperties(mapping));
                indexBuilder.execute().actionGet();

                indexExistingResults(client);

                logInfo("Finished indexing existing published collections").log();
            }

            initialised = true;
        }
    }

    /**
     * index a single published collection.
     *
     * @param publishedCollection
     */
    public void index(Client client, PublishedCollection publishedCollection) throws IOException {

        logInfo("Indexing collection").addParameter("collectionName", publishedCollection.name).log();

        IndexRequestBuilder indexRequest = client.prepareIndex(index, type);
        indexRequest.setSource(Serialiser.serialise(publishedCollection));
        indexRequest.setId(publishedCollection.id);
        ListenableActionFuture<IndexResponse> execution = indexRequest.execute();
        execution.actionGet();
    }

    /**
     * Search published collections
     *
     * @return
     */
    public PublishedCollectionSearchResult search(Client client) throws IOException {
        tryInit(client);

        logInfo("Searching published collections").log();

        PublishedCollectionSearchResult results = new PublishedCollectionSearchResult();

        try {

            SearchResponse response = client.prepareSearch(index)
                    .setTypes(type)
                    .setSearchType(SearchType.QUERY_THEN_FETCH)
                    .setFrom(0)
                    .setSize(20)
                    .addSort(new FieldSortBuilder("publishStartDate").order(SortOrder.DESC))
                    //.setExplain(true)
                    .execute()
                    .actionGet();

            logInfo("Search published collections")
                    .addParameter("found", response.getHits().getTotalHits())
                    .addParameter("returned", response.getHits().getHits().length).log();

            for (SearchHit searchHit : response.getHits()) {
                PublishedCollection collection = Serialiser.deserialise(searchHit.sourceAsString(), PublishedCollection.class);
                results.add(collection);
            }
        } catch (SearchPhaseExecutionException e) {
            logError(e, "Search published collections failed").log();
        }

        return results;
    }

    /**
     * Read existing results from file and add them to elastic search.
     */
    private void indexExistingResults(Client client) throws IOException {

        logInfo("Loading existing published collections from file").log();
        List<PublishedCollection> publishedCollections = readFromFile();

        for (PublishedCollection publishedCollection : publishedCollections) {
            index(client, publishedCollection);
        }
    }

    /**
     * Read all existing published collections from file.
     *
     * @return
     * @throws IOException
     */
    private List<PublishedCollection> readFromFile() throws IOException {
        List<PublishedCollection> publishedCollections = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.json")) {
            for (Path filePath : stream) {
                if (!Files.isDirectory(filePath)) {
                    logInfo("Attempting to read published collection").addParameter("path", filePath.toString())
                            .log();
                    try (InputStream input = Files.newInputStream(filePath)) {
                        publishedCollections.add(Serialiser.deserialise(input,
                                PublishedCollection.class));
                    } catch (IOException e) {
                        logError(e, "Failed to read published collection")
                                .addParameter("path", filePath.toString()).log();
                    }
                }
            }
        }

        return publishedCollections;
    }


    public PublishedCollection add(Path collectionJsonPath) {
        try (InputStream input = Files.newInputStream(collectionJsonPath)) {
            PublishedCollection publishedCollection = Serialiser.deserialise(input,
                    PublishedCollection.class);

            index(ElasticSearchClient.getClient(), publishedCollection);
            return publishedCollection;
        } catch (IOException e) {
            logError(e, "Failed to read published collection")
                    .addParameter("path", collectionJsonPath.toString()).log();
            return null;
        }
    }

    public boolean save(PublishedCollection publishedCollection, Path path) throws IOException {
        synchronized (publishedCollection) {
            try (OutputStream output = Files.newOutputStream(path)) {
                Serialiser.serialise(output, publishedCollection);
                index(ElasticSearchClient.getClient(), publishedCollection);
                return true;
            }
        }
    }
}
