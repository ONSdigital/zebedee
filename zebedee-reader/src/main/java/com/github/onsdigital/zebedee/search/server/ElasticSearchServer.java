package com.github.onsdigital.zebedee.search.server;

import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.zebedee.search.indexing.Indexer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.elasticsearch.client.Client;

import java.util.concurrent.*;

/**
 * Starts an {@link EmbeddedElasticSearchServer} when a client requested
 *
 * @author Bren
 */
public class ElasticSearchServer implements Startup {

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

    public static void startEmbeddedServer() {
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

                        // Index
                        start = System.currentTimeMillis();
                        System.out.println("Elasticsearch: indexing..");
                        try {
                            Indexer.loadIndex(client);
                        } catch (Exception e) {
                            System.out.println("Indexing error");
                            System.out.println(ExceptionUtils.getStackTrace(e));
                            throw e;
                        }
                        System.out.println("Elasticsearch: indexing complete (" + (System.currentTimeMillis() - start) + "ms)");

                        Runtime.getRuntime().addShutdownHook(new ShutDownNodeThread(client, server));
                        return client;
                    }
                });
            }
        }
    }

    @Override
    public void init() {
        startEmbeddedServer();
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
            server.shutdown();
        }
    }

}