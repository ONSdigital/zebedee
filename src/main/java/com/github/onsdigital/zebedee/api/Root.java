package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.ResourceUtils;
import com.github.davidcarboni.restolino.framework.Startup;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.serialiser.IsoDateSerializer;
import com.github.onsdigital.zebedee.model.Content;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Root implements Startup {
    // Environment variables are stored as a static variable so if necessary we can hijack them for testing
    public static Map<String, String> env = System.getenv();
    static final String ZEBEDEE_ROOT = "zebedee_root";


    public static Zebedee zebedee;
    static Path root;

    /**
     * Recursively lists all files within this {@link Content}.
     *
     * @param path  The path to start from. This method calls itself recursively.
     * @param files The list to which results will be added.
     * @throws IOException If a filesystem error occurs.
     */
    private static void listFiles(Path base, Path path, List<Path> files)
            throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    listFiles(base, entry, files);
                } else {
                    files.add(base.relativize(entry));
                }
            }
        }
    }

    @Override
    public void init() {

        // Set ISO date formatting in Gson to match Javascript Date.toISODate()
        Serialiser.getBuilder().registerTypeAdapter(Date.class, new IsoDateSerializer());

        // If we have an environment variable and it is
        String rootDir = env.get(ZEBEDEE_ROOT);
        boolean zebedeeCreated = false;
        if(rootDir != null && rootDir!= "" && Files.exists(Paths.get(rootDir))) {
            root = Paths.get(rootDir);
            try {
                zebedee = Zebedee.create(root);
                zebedeeCreated = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        if(!zebedeeCreated) {
            try {
                // Create a Zebedee folder:
                root = Files.createTempDirectory("zebedee");
                zebedee = Zebedee.create(root);

                // Initialise content folders from bundle
                Path taxonomy = Paths.get(".").resolve(Configuration.getContentDirectory());
                List<Path> content = listContent(taxonomy);
                copyContent(content, taxonomy);
            } catch (IOException | UnauthorizedException e) {
                throw new RuntimeException("Error initialising Zebedee ", e);
            }
        }

        // Set the class that will be used to determine a ClassLoader when loading resources:
        ResourceUtils.classLoaderClass = Root.class;
    }

    private List<Path> listContent(Path taxonomy) throws IOException {
        List<Path> content = new ArrayList<>();

        // List the taxonomy files:
        listFiles(taxonomy, taxonomy, content);

        return content;
    }

    private void copyContent(List<Path> content, Path taxonomy)
            throws IOException {

        // Extract the content:
        // Copy to the master and launchpad content directories
        for (Path item : content) {
            Path source = taxonomy.resolve(item);
            Path masterDestination = zebedee.published.path.resolve(item);
            Path launchpadDestination = zebedee.launchpad.path.resolve(item);
            Files.createDirectories(masterDestination.getParent());
            Files.createDirectories(launchpadDestination.getParent());
            try (InputStream input = Files.newInputStream(source);
                 OutputStream output = Files.newOutputStream(masterDestination)) {
                IOUtils.copy(input, output);
            }
            try (InputStream input = Files.newInputStream(source);
                 OutputStream output = Files.newOutputStream(launchpadDestination)) {
                IOUtils.copy(input, output);
            }
        }

        System.out.println("Zebedee root is at: " + root.toAbsolutePath());
    }

    /**
     * Cleans up
     */
    @Override
    protected void finalize() throws Throwable {
        System.out.println(" - Deleting Zebeddee at: " + root);
        try {
            FileUtils.deleteDirectory(root.toFile());
            System.out.println(" - Deleting Zebeddee complete: " + root);
        } catch (Throwable t) {
            System.out.println(" - Error deleting Zebedee: ");
            System.out.println(t.getStackTrace());
        }
    }

}
