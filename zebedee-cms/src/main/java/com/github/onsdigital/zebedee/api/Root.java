package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.ResourceUtils;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.serialiser.IsoDateSerializer;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.reader.util.AuthorisationHandler;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;

public class Root {
    static final String ZEBEDEE_ROOT = "zebedee_root";
    // Environment variables are stored as a static variable so if necessary we can hijack them for testing
    public static Map<String, String> env = System.getenv();
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

    public static void init() {

        // Set ISO date formatting in Gson to match Javascript Date.toISODate()
        Serialiser.getBuilder().registerTypeAdapter(Date.class, new IsoDateSerializer());

        // Set the class that will be used to determine a ClassLoader when loading resources:
        ResourceUtils.classLoaderClass = Root.class;

        // If we have an environment variable and it is
        String rootDir = env.get(ZEBEDEE_ROOT);
        boolean zebedeeCreated = false;
        if (rootDir != null && rootDir != "" && Files.exists(Paths.get(rootDir))) {
            root = Paths.get(rootDir);
            try {
                zebedee = Zebedee.create(root);
                zebedeeCreated = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        if (!zebedeeCreated) {
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

        //Setting zebedee root as system property for zebedee reader module, since zebedee root is not set as environment variable on develop environment
        System.setProperty("zebedee_root", root.toString());


    }

    private static List<Path> listContent(Path taxonomy) throws IOException {
        List<Path> content = new ArrayList<>();

        // List the taxonomy files:
        listFiles(taxonomy, taxonomy, content);

        return content;
    }

    private static void copyContent(List<Path> content, Path taxonomy)
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

    }

}
