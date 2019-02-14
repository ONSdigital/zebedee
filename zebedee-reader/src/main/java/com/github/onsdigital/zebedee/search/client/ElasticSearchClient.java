package com.github.onsdigital.zebedee.search.client;

import com.github.onsdigital.zebedee.search.configuration.SearchConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.logging.ReaderLogger.info;
import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.getElasticSearchCluster;
import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.getElasticSearchServer;
import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.isStartEmbeddedSearch;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * Starts an {@link EmbeddedElasticSearchServer} when a client requested
 */
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
        return client;
    }

    private static void startEmbeddedServer() throws IOException {
        synchronized (EmbeddedElasticSearchServer.class) {
            if (client == null) {
                long start;

                // Server
                start = System.currentTimeMillis();
                info().log("starting embedded elastic search server");
                EmbeddedElasticSearchServer server = new EmbeddedElasticSearchServer();
                info().data("duration", (System.currentTimeMillis() - start))
                        .log("embedded elastic search server start up complete");

                // Client
                start = System.currentTimeMillis();
                info().log("setting up elastic search client");
                client = server.getClient();
                info().data("duration", (System.currentTimeMillis() - start))
                        .log("elastic searcg client set up completed");

                Runtime.getRuntime().addShutdownHook(new ShutDownNodeThread(client, server));
            }
        }
    }

    public static void init() throws IOException {
        if (isStartEmbeddedSearch()) {
            startEmbeddedServer();
        } else {
            info().log("embedded elastic search not started as it configuration is disabled");
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

        client = TransportClient.builder().settings(settings).build()
                .addTransportAddress(new InetSocketTransportAddress(
                        new InetSocketAddress(getElasticSearchServer(),
                                SearchConfiguration.getElasticSearchPort())));
    }


    private static void initNodeClient() throws IOException {
        searchHome = Files.createTempDirectory("zebedee_search_client");
        Settings settings = Settings.builder().put("http.enabled", false)
                .put("cluster.name", getElasticSearchCluster())
                .put("discovery.zen.ping.multicast.enabled", true)
                .put("path.home", searchHome).build();
        Node node =
                nodeBuilder()
                        .settings(settings)
                        .data(false)
                        .node();

        client = node.client();
        Runtime.getRuntime().addShutdownHook(new ShutDownNodeThread(client, null));
    }

    static class ShutDownNodeThread extends Thread {

        private Client client;
        private EmbeddedElasticSearchServer server;

        public ShutDownNodeThread(Client client, EmbeddedElasticSearchServer server) {
            this.client = client;
            this.server = server;
        }

        @Override
        public void run() {

            client.close();

            // Once we get the client, the server
            // is guaranteed to have been created:
            if (server != null) {
                server.shutdown();
            }

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