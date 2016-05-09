package com.github.onsdigital.zebedee.search.indexing;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.elasticSearchLog;
import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.getSearchAlias;

/**
 * Created by bren on 02/09/15.
 */
class ElasticSearchUtils {
    private Client client;
    private String DEFAULT_TYPE = "_default_";

    public ElasticSearchUtils(Client client) {
        this.client = client;
    }


    /**
     * Creates index with default mapping
     *
     * @param index
     * @param settings
     * @param defaultMappingSource
     * @return
     * @throws IOException
     */
    public CreateIndexResponse createIndex(String index, Settings settings, String defaultMappingSource) throws IOException {
        return createIndex(index, settings, null, defaultMappingSource);
    }

    /**
     * Create index with given type mapping
     *
     * @param index
     * @param settings
     * @param type
     * @param mappingSource
     * @return
     * @throws IOException
     */
    public CreateIndexResponse createIndex(String index, Settings settings, String type, String mappingSource) throws IOException {
        elasticSearchLog("Creating index").addParameter("index", index).log();
        CreateIndexRequestBuilder createIndexRequest = getIndicesClient().prepareCreate(index);
        createIndexRequest.setSettings(settings);
        if (type == null) {
            createIndexRequest.addMapping(DEFAULT_TYPE, mappingSource);
        } else {
            createIndexRequest.addMapping(type, mappingSource);
        }
        return createIndexRequest.get();
    }

    public DeleteIndexResponse deleteIndex(String index) {
        elasticSearchLog("Deleting index").addParameter("index", index).log();
        DeleteIndexRequestBuilder deleteIndexRequest = getIndicesClient().prepareDelete(index);
        return deleteIndexRequest.get();
    }

    /**
     * @param index index to add alias to
     * @param alias alias name
     * @return
     */
    public IndicesAliasesResponse addAlias(String index, String alias) {
        elasticSearchLog("Adding alias").addParameter("alias", alias).addParameter("index", index).log();
        IndicesAliasesRequestBuilder addAliasRequest = getAliasesBuilder().addAlias(index, alias);
        return addAliasRequest.get();
    }

    /**
     * @param index index to remove alias from
     * @param alias
     * @return
     */
    public IndicesAliasesResponse removeAlias(String index, String alias) {
        elasticSearchLog("Removing alias").addParameter("alias", alias).addParameter("index", index).log();
        IndicesAliasesRequestBuilder removeAliasRequest = getAliasesBuilder().removeAlias(index, alias);
        return removeAliasRequest.get();
    }


    /**
     * Swaps alias referring to old index to a new one.
     *
     * @param oldIndex Old index name, if null will not attempt removing alias from old index
     * @param newIndex
     * @param alias
     */
    public void swapIndex(String oldIndex, String newIndex, String alias) {
        IndicesAliasesRequestBuilder aliasBuilder = getAliasesBuilder();
        aliasBuilder.addAlias(newIndex, alias);
        if (oldIndex != null) {
            aliasBuilder.removeAlias(oldIndex, getSearchAlias());
        }
        aliasBuilder.get();
    }


    /**
     * Creates given document under given index and given type, if document wit the id already exists overwrites the existing one
     *
     * @param index
     * @param type
     * @param id
     * @param document
     * @return
     */
    public IndexResponse createDocument(String index, String type, String id, String document) {
        IndexRequestBuilder indexRequestBuilder = prepareIndex(index, type, id);
        indexRequestBuilder.setSource(document);
        return indexRequestBuilder.get();
    }

    public DeleteResponse deleteDocument(String index, String type, String id) {
        DeleteRequestBuilder deleteRequestBuilder = client.prepareDelete(index, type, id);
        return deleteRequestBuilder.get();
    }

    public boolean isIndexAvailable(String index) {
        IndicesExistsResponse response = client.admin().indices().prepareExists(index).execute().actionGet();
        return response.isExists();
    }

    //Returns first index of alias, ons alias will not have more than one index at any time
    public String getAliasIndex(String alias) {
        GetAliasesResponse getAliasesResponse = client.admin().indices().prepareGetAliases(alias).get();
        ImmutableOpenMap<String, List<AliasMetaData>> aliases = getAliasesResponse.getAliases();
        if (aliases.isEmpty()) {
            return null;
        }
        ObjectObjectCursor<String, List<AliasMetaData>> next = aliases.iterator().next();
        return next.key;
    }

    public IndexRequestBuilder prepareIndex(String index, String type, String id) {
        return client.prepareIndex(index, type, id);
    }

    private IndicesAdminClient getIndicesClient() {
        return client.admin().indices();
    }

    private IndicesAliasesRequestBuilder getAliasesBuilder() {
        return getIndicesClient().prepareAliases();
    }

    private ListenableActionFuture execute(ActionRequestBuilder requestBuilder) {
        return requestBuilder.execute();
    }

}
