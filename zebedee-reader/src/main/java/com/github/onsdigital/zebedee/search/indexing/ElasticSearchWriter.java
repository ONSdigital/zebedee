package com.github.onsdigital.zebedee.search.indexing;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Map;

import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.getElasticSearchIndexAlias;

/**
 * Created by bren on 02/09/15.
 */
class ElasticSearchWriter {
    private Client client;
    private String DEFAULT_TYPE = "_default_";

    public ElasticSearchWriter(Client client) {
        this.client = client;
    }


    public CreateIndexResponse createIndex(String index, Settings settings, String defaultMappingSource) throws IOException {
        System.out.println("Creating index " + index);
        CreateIndexRequestBuilder createIndexRequest = getIndicesClient().prepareCreate(index);
        createIndexRequest.setSettings(settings);
        createIndexRequest.addMapping(DEFAULT_TYPE, defaultMappingSource);
        return createIndexRequest.get();
    }

    public DeleteIndexResponse deleteIndex(String index) {
        System.out.println("Deleting index " + index);
        DeleteIndexRequestBuilder deleteIndexRequest = getIndicesClient().prepareDelete(index);
        return deleteIndexRequest.get();
    }

    /**
     * @param index index to add alias to
     * @param alias alias name
     * @return
     */
    public IndicesAliasesResponse addAlias(String index, String alias) {
        System.out.printf("Adding alias %s to index %s", alias, index);
        IndicesAliasesRequestBuilder addAliasRequest = getAliasesBuilder().addAlias(index, alias);
        return addAliasRequest.get();
    }

    /**
     * @param index index to remove alias from
     * @param alias
     * @return
     */
    public IndicesAliasesResponse removeAlias(String index, String alias) {
        System.out.printf("Removing alias %s of index %s", alias, index);
        IndicesAliasesRequestBuilder removeAliasRequest = getAliasesBuilder().removeAlias(index, alias);
        return removeAliasRequest.get();
    }


    /***
     *
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
            aliasBuilder.removeAlias(oldIndex, getElasticSearchIndexAlias());
        }
        aliasBuilder.get();
    }


    /**
     *
     * Creates given document under given index and given type, if document wit the id already exists overwrites the existing one
     *
     * @param index
     * @param type
     * @param id
     * @param document
     * @return
     */
    public IndexResponse createDocument(String index,String type, String id, String document) {
        IndexRequestBuilder indexRequestBuilder = client.prepareIndex(index, type, id);
        indexRequestBuilder.setSource(document);
        return indexRequestBuilder.get();
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
