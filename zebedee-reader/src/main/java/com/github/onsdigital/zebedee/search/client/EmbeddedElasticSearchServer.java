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

import static com.github.onsdigital.zebedee.logging.ReaderLogger.error;
import static com.github.onsdigital.zebedee.logging.ReaderLogger.info;

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
        info().data("data_dir", this.dataDirectory.toString()).log("creating elastic search index data");

        if (settings != null) {
            settingsBuilder.put(settings);
            // If data directory is overwritten update data directory
            // accordingly for cleanup at shutdown
            String directory = settings.get("path.data");
            if (directory != null) {
                this.dataDirectory = Paths.get(directory);
            }
        }

        info().data("settings", settingsBuilder.internalMap()).log("starting embedded elastic search node");
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
            info().data("dir", dataDirectory.toString()).log("embedeed elastic search: deleting data directory");
            FileUtils.deleteDirectory(dataDirectory.toFile());
        } catch (IOException e) {
            throw new RuntimeException(error()
                    .logException(e, "Could not delete data directory of embedded elasticsearch server"));
        }
    }
}
