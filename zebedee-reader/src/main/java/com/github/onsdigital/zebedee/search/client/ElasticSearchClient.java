package com.github.onsdigital.zebedee.search.client;

import com.github.onsdigital.elasticutils.util.ElasticSearchHelper;
import com.github.onsdigital.zebedee.search.configuration.SearchConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.elasticSearchLog;
import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.*;

public class ElasticSearchClient {

    static Client client;
    private static Path searchHome;

    /**
     * NB caching the client for the entire application to use is safe and
     * recommended:
     * <p>
     * <a href=
     * "http://stackoverflow.com/questions/15773476/elasticsearch-client-thread-safety"
     * >http://stackoverflow.com/questions/15773476/elasticsearch-client-thread-
     * safety</a>
     *
     * @return
     */
    public static Client getClient() {
        if (client == null) {
            try {
                init();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return client;
    }

    public static void init() throws IOException {
        if (isStartEmbeddedSearch()) {
            elasticSearchLog("Starting embedded search server").log();
            connect();
        } else {
            elasticSearchLog("Not starting search server due to configuration parameters").log();
            connect();
        }
    }

    private static void connect() throws IOException {
        if (client == null) {
            initTransportClient();
        }
    }

    protected static void initTransportClient() throws IOException {
        Settings.Builder builder = Settings.builder();

        if (!StringUtils.isBlank(getElasticSearchCluster()))
            builder.put("cluster.name", getElasticSearchCluster());

        Settings settings = builder.build();

        client = ElasticSearchHelper.getTransportClient(getElasticSearchServer(), SearchConfiguration.getElasticSearchPort(), settings);
        Runtime.getRuntime().addShutdownHook(new ShutDownNodeThread(client));
    }

    static class ShutDownNodeThread extends Thread {

        private Client client;

        public ShutDownNodeThread(Client client) {
            this.client = client;
        }

        @Override
        public void run() {

            client.close();

            if (searchHome != null) {
                try {
                    Files.deleteIfExists(searchHome);
                } catch (IOException e) {
                    System.err.println("Falied cleaning temporary search client directory");
                    e.printStackTrace();
                }
            }
        }
    }

}