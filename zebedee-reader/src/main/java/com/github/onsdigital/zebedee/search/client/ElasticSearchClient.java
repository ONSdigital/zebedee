package com.github.onsdigital.zebedee.search.client;

import com.github.onsdigital.zebedee.search.configuration.SearchConfiguration;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.*;
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
                System.out.println("Elasticsearch: starting embedded server..");
                EmbeddedElasticSearchServer server = new EmbeddedElasticSearchServer();
                System.out.println("Elasticsearch: embedded server started (" + (System.currentTimeMillis() - start) + "ms)");

                // Client
                start = System.currentTimeMillis();
                System.out.println("Elasticsearch: creating client..");
                client = server.getClient();
                System.out.println("Elasticsearch: client set up (" + (System.currentTimeMillis() - start) + "ms)");

                Runtime.getRuntime().addShutdownHook(new ShutDownNodeThread(client, server));
            }
        }
    }

    public static void init() throws IOException {
        if (isStartEmbeddedSearch()) {
            System.out.println("Starting embedded elastic search server");
            startEmbeddedServer();
        } else {
            System.out.println("Not starting elastic search server due to configuration parameters");
            connect();
        }
    }

    private static void connect() throws IOException {
        if (client == null) {
            initTransportClient();
//            initNodeClient();
        }
    }


    protected static void initTransportClient() throws IOException {
        Settings settings = Settings.builder()
                .put("cluster.name", getElasticSearchCluster()).build();
        client = TransportClient.builder().settings(settings).build()
                .addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress(getElasticSearchServer(), SearchConfiguration.getElasticSearchPort())));
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