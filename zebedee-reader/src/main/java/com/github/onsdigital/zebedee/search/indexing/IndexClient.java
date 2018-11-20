package com.github.onsdigital.zebedee.search.indexing;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;

import java.io.IOException;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.elasticSearchLog;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;

/**
 * Created by David Sullivan on 15/11/18
 */
public class IndexClient {

    private static IndexClient INSTANCE;

    public static IndexClient getInstance() {
        if (INSTANCE == null) {
            synchronized (IndexClient.class) {
                if (INSTANCE == null) {
                    INSTANCE = new IndexClient();
                }
            }
        }
        return INSTANCE;
    }

    private final Client client;

    private IndexClient() {
        try {
            if (ElasticSearchClient.getClient() == null) {
                ElasticSearchClient.init();
            }
        } catch (IOException e) {
            logError(e)
                    .addMessage("Caught exception initialising Elasticsearch client")
                    .log();
            throw new RuntimeException(e);
        }
        this.client = ElasticSearchClient.getClient();
    }

    private IndicesAdminClient getIndexClient() {
        return this.client.admin().indices();
    }

    /**
     * Build and execute a request to create a new index
     * @param index
     * @param settings
     * @param type
     * @param mappingSource
     * @return
     */
    public CreateIndexResponse createIndex(String index, Settings settings, String type, String mappingSource) {
        elasticSearchLog("Creating index").addParameter(Parameters.INDEX.parameter, index).log();

        CreateIndexRequestBuilder requestBuilder = this.getIndexClient()
                .prepareCreate(index)
                .setSettings(settings)
                .addMapping(type, mappingSource);

        return requestBuilder.get();
    }

    /**
     * Build and execute a request to delete an index
     * @param index
     * @return
     */
    public DeleteIndexResponse deleteIndex(String index) {
        elasticSearchLog("Deleting index").addParameter(Parameters.INDEX.parameter, index).log();

        DeleteIndexRequestBuilder requestBuilder = this.getIndexClient()
                .prepareDelete(index);

        return requestBuilder.get();
    }

    /**
     * Build and execute a request to add the default index alias
     * @param index
     * @return
     */
    public IndicesAliasesResponse addDefaultIndexAlias(String index) {
        return this.addIndexAlias(index, Index.ONS.getIndex());
    }

    /**
     * Build and execute a request to add an index alias
     * @param index
     * @param alias
     * @return
     */
    public IndicesAliasesResponse addIndexAlias(String index, String alias) {
        elasticSearchLog("Adding index alias")
                .addParameter(Parameters.INDEX.parameter, index)
                .addParameter(Parameters.ALIAS.parameter, alias)
                .log();

        IndicesAliasesRequestBuilder requestBuilder = this.getIndexClient()
                .prepareAliases()
                .addAlias(index, alias);

        return requestBuilder.get();
    }

    /**
     * Build and execute a request to remove the default index alias
     * @param index
     * @return
     */
    public IndicesAliasesResponse removeDefaultIndexAlias(String index) {
        return this.removeIndexAlias(index, Index.ONS.getIndex());
    }

    /**
     * Build and execute a request to remove an index alias
     * @param index
     * @param alias
     * @return
     */
    public IndicesAliasesResponse removeIndexAlias(String index, String alias) {
        elasticSearchLog("Removing index alias")
                .addParameter(Parameters.INDEX.parameter, index)
                .addParameter(Parameters.ALIAS.parameter, alias)
                .log();

        IndicesAliasesRequestBuilder requestBuilder = this.getIndexClient()
                .prepareAliases()
                .removeAlias(index, alias);

        return requestBuilder.get();
    }

    /**
     * Swap alias referring to old index to a new one.
     * @param oldIndex
     * @param newIndex
     * @param alias
     */
    public void swapIndexAlias(String oldIndex, String newIndex, String alias) {
        this.addIndexAlias(newIndex, alias);
        if (oldIndex != null) {
            this.removeIndexAlias(oldIndex, alias);
        }
    }

    /**
     * Serialise a document and creates an IndexRequestBuilder
     * @param index
     * @param type
     * @param id
     * @param document
     * @return
     */
    public IndexRequestBuilder createDocumentIndexRequest(String index, String type, String id, Object document) {
        String payload = document instanceof String ? (String) document : ContentUtil.serialise(document);
        return this.client
                .prepareIndex(index, type, id)
                .setSource(payload);
    }

    /**
     * Indexes a document under the given index and document type.
     * Overwrites existing documents.
     * Warning!! Document types are deprecation in Elasticsearch 5.X.X and removed in
     * Elasticsearch 6.X.X
     * @param index
     * @param type
     * @param id
     * @param document
     * @return
     */
    public IndexResponse createDocument(String index, String type, String id, Object document) {
        return this.createDocumentIndexRequest(index, type, id, document).get();
    }

    /**
     * Creates the global lock document
     * @return
     */
    public IndexResponse createGlobalLockDocument() {
        return this.createDocument("fs", "lock", "global", "{}");
    }

    /**
     * Deletes the global lock document
     * @return
     */
    public DeleteResponse deleteGlobalLockDocument() {
        return this.deleteDocument("fs", "lock", "global");
    }

    /**
     * Delete a document in the given index with the given type and id
     * @param index
     * @param type
     * @param id
     * @return
     */
    public DeleteResponse deleteDocument(String index, String type, String id) {
        DeleteRequestBuilder requestBuilder = this.client
                .prepareDelete(index, type, id);

        return requestBuilder.get();
    }

    /**
     * Returns true if the given index exists, else false
     * @param index
     * @return
     */
    public boolean indexExists(String index) {
        IndicesExistsRequestBuilder requestBuilder = this.getIndexClient()
                .prepareExists(index);

        IndicesExistsResponse response = requestBuilder.get();
        return response.isExists();
    }

    /**
     * Returns the index that the given alias points to
     * @param alias
     * @return
     */
    public String getIndexForAlias(String alias) {
        GetAliasesRequestBuilder requestBuilder = this.getIndexClient()
                .prepareGetAliases(alias);

        GetAliasesResponse response = requestBuilder.get();

        ImmutableOpenMap<String, List<AliasMetaData>> aliases = response.getAliases();
        if (aliases.isEmpty()) {
            return null;
        }
        ObjectObjectCursor<String, List<AliasMetaData>> next = aliases.iterator().next();
        return next.key;
    }

    public BulkProcessor getBulkProcessor() {
        return BulkProcessor.builder(
                this.client,
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
    }

    public enum Parameters {
        INDEX("index"),
        ALIAS("alias");

        String parameter;

        Parameters(String parameter) {
            this.parameter = parameter;
        }

        public String getParameter() {
            return parameter;
        }
    }

}
