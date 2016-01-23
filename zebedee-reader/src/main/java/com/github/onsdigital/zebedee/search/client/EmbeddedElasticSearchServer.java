package com.github.onsdigital.zebedee.search.client;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class EmbeddedElasticSearchServer {

    private static final String DEFAULT_CLUSTERNAME = "ONSCluster";
    private final Node node;
    private Path dataDirectory;

    public EmbeddedElasticSearchServer() throws IOException {
        this(null, DEFAULT_CLUSTERNAME);
    }

    public EmbeddedElasticSearchServer(Settings settings, String clusterName) throws IOException {

        this.dataDirectory = Files.createTempDirectory("searchindex");
        Settings.Builder settingsBuilder = Settings.builder().put("cluster.name", clusterName).put("http.enabled", true).put("path.home", dataDirectory)
                .put("node.data", true);
        System.out.println("Creating index data in: " + this.dataDirectory);

        if (settings != null) {
            settingsBuilder.put(settings);
            // If data directory is overwritten update data directory
            // accordingly for cleanup at shutdown
            String directory = settings.get("path.data");
            if (directory != null) {
                this.dataDirectory = Paths.get(directory);
            }
        }

        System.out.println("Starting embedded Elastic Search node with settings" + settingsBuilder.internalMap());
        node = NodeBuilder.nodeBuilder().local(false).settings(settingsBuilder.build()).node();
    }

    public Client getClient() {
        return node.client();
    }

    public void shutdown() {
        node.close();
        deleteDataDirectory();
    }

    private void deleteDataDirectory() {
        try {
            System.out.println("Deleting data directory: " + dataDirectory);
            FileUtils.deleteDirectory(dataDirectory.toFile());
            System.out.println("Finished deleting " + dataDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete data directory of embedded elasticsearch server", e);
        }
    }
}
