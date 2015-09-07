package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollection;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollectionSearchResult;
import com.github.onsdigital.zebedee.json.publishing.Result;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import com.github.onsdigital.zebedee.util.Log;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
                Log.print("Creating search index for published collections.");
                CreateIndexRequestBuilder indexBuilder = client.admin().indices()
                        .prepareCreate(index);

                Log.print("Adding mapping for published collections index");
                indexBuilder.addMapping(mapping, getMappingProperties(mapping));
                indexBuilder.execute().actionGet();

                indexExistingResults(client);

                Log.print("Finished indexing existing published collections");
            }

            initialised = true;
        }
    }

    /**
     * index a single published collection.
     *
     * @param publishedCollection
     */
    private void index(Client client, PublishedCollection publishedCollection) throws IOException {

        Log.print("Indexing collection %s", publishedCollection.name);

        IndexRequestBuilder indexRequest = client.prepareIndex(index, type);
        indexRequest.setSource(Serialiser.serialise(publishedCollection));
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

        Log.print("Searching published collections");

        PublishedCollectionSearchResult results = new PublishedCollectionSearchResult();

        try {

            SearchResponse response = client.prepareSearch(index)
                    .setTypes(type)
                    .setFrom(0)
                    .setSize(50)
                    .addSort(new FieldSortBuilder("publishDate").order(SortOrder.DESC))
                            //.setExplain(true)
                    .execute()
                    .actionGet();

            Log.print("Found %d published collections. Returned %d results", response.getHits().getTotalHits(), response.getHits().getHits().length);

            for (SearchHit searchHit : response.getHits()) {
                PublishedCollection collection = Serialiser.deserialise(searchHit.sourceAsString(), PublishedCollection.class);
                results.add(collection);
            }
        } catch (SearchPhaseExecutionException e) {
            Log.print("Search failed");
            ExceptionUtils.printRootCauseStackTrace(e);
        }

        return results;
    }


    /**
     * Read existing results from file and add them to elastic search.
     */
    private void indexExistingResults(Client client) throws IOException {

        Log.print("Loading existing published collections from file");
        List<PublishedCollection> publishedCollections = readFromFile();

        for (PublishedCollection publishedCollection : publishedCollections) {
            index(client, publishedCollection);
        }
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

    /**
     * Read all existing published collections from file.
     *
     * @return
     * @throws IOException
     */
    private List<PublishedCollection> readFromFile() throws IOException {
        List<PublishedCollection> publishedCollections = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path filePath : stream) {
                if (!Files.isDirectory(filePath)) {
                    try (InputStream input = Files.newInputStream(filePath)) {
                        publishedCollections.add(Serialiser.deserialise(input,
                                PublishedCollection.class));
                    } catch (IOException e) {
                        Log.print("Failed to read published collection with path %s", filePath.toString());
                    }
                }
            }
        }

        return publishedCollections;
    }


    public void add(Path collectionJsonPath) {
        try (InputStream input = Files.newInputStream(collectionJsonPath)) {
            PublishedCollection publishedCollection = Serialiser.deserialise(input,
                    PublishedCollection.class);

            index(ElasticSearchClient.getClient(), publishedCollection);
        } catch (IOException e) {
            Log.print("Failed to read published collection with path %s", collectionJsonPath.toString());
        }
    }
}
