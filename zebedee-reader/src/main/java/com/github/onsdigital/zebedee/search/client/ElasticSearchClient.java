package com.github.onsdigital.zebedee.search.client;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.util.concurrent.*;

import static com.github.onsdigital.zebedee.search.configuration.SearchConfiguration.*;

/**
 * Starts an {@link EmbeddedElasticSearchServer} when a client requested
 */
public class ElasticSearchClient {

    static ExecutorService pool = Executors.newSingleThreadExecutor();

    static Future<Client> client;

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
        try {
            return client.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error getting Elasticsearch client - indexing may have failed", e);
        }
    }

    private static void startEmbeddedServer() {
        synchronized (EmbeddedElasticSearchServer.class) {
            if (client == null) {
                client = pool.submit(new Callable<Client>() {
                    @Override
                    public Client call() throws Exception {
                        long start;

                        // Server
                        start = System.currentTimeMillis();
                        System.out.println("Elasticsearch: starting embedded server..");
                        EmbeddedElasticSearchServer server = new EmbeddedElasticSearchServer();
                        System.out.println("Elasticsearch: embedded server started (" + (System.currentTimeMillis() - start) + "ms)");

                        // Client
                        start = System.currentTimeMillis();
                        System.out.println("Elasticsearch: creating client..");
                        Client client = server.getClient();
                        System.out.println("Elasticsearch: client set up (" + (System.currentTimeMillis() - start) + "ms)");

                        Runtime.getRuntime().addShutdownHook(new ShutDownNodeThread(client, server));
                        return client;
                    }
                });
            }
        }
    }

    public static void init() {
        if (isStartEmbeddedSearch()) {
            System.out.println("Starting embedded elastic search server");
            startEmbeddedServer();
        } else {
            System.out.println("Not starting elastic search server due to configuration parameters");
            connect();
        }
    }

    private static void connect() {
        if (client == null) {
            client = pool.submit(new Callable<Client>() {
                @Override
                public Client call() throws Exception {

                    Settings settings = ImmutableSettings.settingsBuilder()
                            .put("cluster.name", getElasticSearchCluster()).build();

                    Client client = new TransportClient(settings)
                            .addTransportAddress(new InetSocketTransportAddress(getElasticSearchServer(), getElasticSearchPort()));

                    Runtime.getRuntime().addShutdownHook(new ShutDownNodeThread(client, null));
                    return client;
                }
            });
        }


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
        }
    }

}