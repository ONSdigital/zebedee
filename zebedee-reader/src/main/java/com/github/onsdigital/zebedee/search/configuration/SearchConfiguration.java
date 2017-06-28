package com.github.onsdigital.zebedee.search.configuration;

import static com.github.onsdigital.zebedee.util.VariableUtils.getVariableValue;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * Created by bren on 02/09/15.
 */
public class SearchConfiguration {

    private static String elasticSearchServer = defaultIfBlank(getVariableValue("ELASTIC_SEARCH_SERVER"), "localhost");
    private static String elasticSearchAlias = defaultIfBlank(getVariableValue("ELASTIC_SEARCH_ALIAS"), "ons");
    private static Integer elasticSearchPort = Integer.parseInt(defaultIfBlank(getVariableValue
            ("ELASTIC_SEARCH_PORT"), "9300"));
    private static String elasticSearchCluster = defaultIfBlank(getVariableValue("ELASTIC_SEARCH_CLUSTER"), "");
    private static boolean startEmbeddedSearch = "Y".equals(defaultIfBlank(getVariableValue("START_EMBEDDED_SERVER"), "N"));

    public static boolean isStartEmbeddedSearch() {
        return startEmbeddedSearch;
    }
    public static String getSearchAlias() {
        return elasticSearchAlias;
    }

    public static String getElasticSearchServer() {
        return elasticSearchServer;
    }

    public static Integer getElasticSearchPort() {
        return elasticSearchPort;
    }

    public static String getElasticSearchCluster() {
        return elasticSearchCluster;
    }


}
