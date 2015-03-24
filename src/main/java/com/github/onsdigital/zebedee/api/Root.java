package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.ResourceUtils;
import com.github.davidcarboni.restolino.framework.Startup;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.model.Content;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Root implements Startup {

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
        try {

            // Create a Zebedee folder:
            root = Files.createTempDirectory("zebedee");
            zebedee = Zebedee.create(root);
            zebedee.permissions.addEditor("florence@magicroundabout.ons.gov.uk");
            Path taxonomy = Paths.get(".").resolve("target/taxonomy");
            List<Path> content = listContent(taxonomy);
            copyContent(content, taxonomy);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Set ISO date formatting in Gson to match Javascript Date.toISODate()
        Serialiser.getBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

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
        for (Path item : content) {
            Path source = taxonomy.resolve(item);
            Path destination = zebedee.published.path.resolve(item);
            Files.createDirectories(destination.getParent());
            try (InputStream input = Files.newInputStream(source);
                 OutputStream output = Files.newOutputStream(destination)) {
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
