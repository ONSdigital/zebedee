package com.github.onsdigital.zebedee;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.PathUtils;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link Deprecated} Please do not use this any more.
 */
@Deprecated
public class Builder {

    public String[] collectionNames = {"Inflation Q2 2015", "Labour Market Q2 2015"};
    public Path parent;
    public Path zebedeeRootPath;
    public List<Path> collections;

    private Zebedee zebedee;

    /**
     * Constructor to build a known {@link Zebedee} structure with minimal structure for testing.
     *
     * @throws IOException
     */
    public Builder() throws IOException {

        // Create the structure:
        parent = Files.createTempDirectory(Random.id());
        zebedeeRootPath = Files.createDirectory(parent.resolve(Zebedee.ZEBEDEE));
        Files.createDirectory(zebedeeRootPath.resolve(Zebedee.COLLECTIONS));

        // Create the collections:
        collections = new ArrayList<>();
        for (String collectionName : collectionNames) {
            Path collection = createCollection(collectionName, zebedeeRootPath);
            collections.add(collection);
        }

        // Set up some permissions:
        Path permissions = zebedeeRootPath.resolve(Zebedee.PERMISSIONS);
        Files.createDirectories(permissions);

        ZebedeeConfiguration configuration = new ZebedeeConfiguration(parent);
        this.zebedee = new Zebedee(configuration);
    }

    /**
     * Constructor to build an instance of zebedee using a predefined set of content
     *
     * @param bootStrap
     * @throws IOException
     */
    public Builder(Path bootStrap) throws IOException {
        this();

        FileUtils.deleteDirectory(this.zebedeeRootPath.resolve(Zebedee.COLLECTIONS).toFile());
        Files.createDirectory(this.zebedeeRootPath.resolve(Zebedee.COLLECTIONS));

        FileUtils.copyDirectory(bootStrap.resolve(Zebedee.PUBLISHED).toFile(), this.zebedeeRootPath.resolve(Zebedee.PUBLISHED).toFile());
    }

    public void delete() throws IOException {
        FileUtils.deleteDirectory(parent.toFile());
    }

    /**
     * This method creates the expected set of folders for a Zebedee structure.
     * This code is intentionally copied from
     * <p>
     * This ensures there's a fixed expectation, rather than relying on a method that will be tested as part
     * of the test suite.
     *
     * @param root The root of the {@link Zebedee} structure
     * @param name The name of the {@link com.github.onsdigital.zebedee.model.Collection}.
     * @return The root {@link com.github.onsdigital.zebedee.model.Collection} path.
     * @throws IOException If a filesystem error occurs.
     */
    private Path createCollection(String name, Path root) throws IOException {

        String filename = PathUtils.toFilename(name);
        Path collections = root.resolve(Zebedee.COLLECTIONS);

        // Create the folders:
        Path collection = collections.resolve(filename);
        Files.createDirectory(collection);
        Files.createDirectory(collection.resolve(com.github.onsdigital.zebedee.model.Collection.REVIEWED));
        Files.createDirectory(collection.resolve(com.github.onsdigital.zebedee.model.Collection.COMPLETE));
        Files.createDirectory(collection.resolve(com.github.onsdigital.zebedee.model.Collection.IN_PROGRESS));

        // Create the description:
        Path collectionDescription = collections.resolve(filename + ".json");
        CollectionDescription description = new CollectionDescription();
        description.setId(Random.id());
        description.setName(name);
        try (OutputStream output = Files.newOutputStream(collectionDescription)) {
            Serialiser.serialise(output, description);
        }

        return collection;
    }

    public Zebedee getZebedee() {
        return zebedee;
    }
}
