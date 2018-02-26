package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.content.util.ContentConstants;
import com.github.onsdigital.zebedee.content.util.IsoDateSerializer;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollection;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollectionSearchResult;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Represents the store of published collections for adding to and searching.
 */
public class PublishedCollections {

    private static final String index = "publishedcollections";
    private static final String type = "collection";
    private static final IsoDateSerializer dateSerialiser = new IsoDateSerializer(ContentConstants.JSON_DATE_PATTERN);
    public final Path path;
    private boolean initialised = false;

    public PublishedCollections(Path path) {
        this.path = path;
    }

    public static void main(String[] args) throws ParseException {
        Date date = dateSerialiser.deserialize("2016-05-25T11:54:10.521Z");
        System.out.println("date = " + date);
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

        logInfo("Indexing collection").addParameter("collectionName", publishedCollection.getName()).log();

        IndexRequestBuilder indexRequest = client.prepareIndex(index, type);
        indexRequest.setSource(Serialiser.serialise(publishedCollection));
        indexRequest.setId(publishedCollection.getId());
        ListenableActionFuture<IndexResponse> execution = indexRequest.execute();
        execution.actionGet();
    }

    public PublishedCollectionSearchResult search(Client client) throws IOException {
        tryInit(client);

        logInfo("Searching published collections").log();

        PublishedCollectionSearchResult results = new PublishedCollectionSearchResult();

        try {

            SearchRequestBuilder requestBuilder = client.prepareSearch(index)
                    .setTypes(type)
                    .setSearchType(SearchType.QUERY_THEN_FETCH)
                    .setFrom(0)
                    .setSize(100)
                    .addFields(new String[]{"id", "name", "type", "publishStartDate"})
                    .addSort(new FieldSortBuilder("publishStartDate").order(SortOrder.DESC));
            //.setExplain(true)

            SearchResponse response = requestBuilder.execute()
                    .actionGet();

            logInfo("Search published collections")
                    .addParameter("found", response.getHits().getTotalHits())
                    .addParameter("returned", response.getHits().getHits().length).log();

            for (SearchHit searchHit : response.getHits()) {

                String publishStartDate = "";
                try {
                    publishStartDate = searchHit.field("publishStartDate").getValue().toString();
                } catch (NullPointerException e) {
                } // leave as default value

                if (StringUtils.isNotEmpty(publishStartDate)) {
                    PublishedCollection collection = new PublishedCollection(
                            searchHit.field("id").getValue().toString(),
                            searchHit.field("name").getValue().toString(),
                            CollectionType.valueOf(searchHit.field("type").getValue().toString()),
                            dateSerialiser.deserialize(publishStartDate));
                    results.add(collection);
                }
            }
        } catch (SearchPhaseExecutionException | ParseException e) {
            logError(e, "Search published collections failed").log();
        }

        return results;
    }

    /**
     * Search published collections
     *
     * @return
     */
    public PublishedCollectionSearchResult search(Client client, String collectionId) throws IOException {
        tryInit(client);

        logInfo("Searching for published collection").addParameter("collectionId", collectionId).log();

        PublishedCollection result = new PublishedCollection();

        try {

            SearchRequestBuilder requestBuilder = client.prepareSearch(index)
                    .setTypes(type)
                    .setSearchType(SearchType.QUERY_THEN_FETCH)
                    .setFrom(0)
                    .setSize(20)
                    .addSort(new FieldSortBuilder("publishStartDate").order(SortOrder.DESC));
            //.setExplain(true)

            if (collectionId != null && collectionId.length() > 0) {
                requestBuilder.setQuery(QueryBuilders.matchQuery("id", collectionId));
            }

            SearchResponse response = requestBuilder.execute()
                    .actionGet();

            logInfo("Search published collections")
                    .addParameter("found", response.getHits().getTotalHits())
                    .addParameter("returned", response.getHits().getHits().length).log();

            for (SearchHit searchHit : response.getHits()) {
                result = Serialiser.deserialise(searchHit.sourceAsString(), PublishedCollection.class);
            }
        } catch (SearchPhaseExecutionException e) {
            logError(e, "Search published collections failed").log();
        }

        PublishedCollectionSearchResult results = new PublishedCollectionSearchResult();
        results.add(result);

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
