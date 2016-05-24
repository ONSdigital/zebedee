package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.api.DataVisualisationZip;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.visualisation.Visualisation;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VisualisationImporter {

    public static void main(String[] args) throws IOException {

        Path source = Paths.get("/Users/carlhembrough/Downloads/live");
        Path destination = Paths.get("/Users/carlhembrough/Downloads/generated");

        importVisualisations(source, destination);
    }

    public static void importVisualisations(Path sourceRoot, Path destinationRoot) throws IOException {

        // for each directory
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(sourceRoot)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    try {
                        System.out.println("****** source = " + path);
                        Path relative = sourceRoot.relativize(path);
                        System.out.println("relative = " + relative);
                        ImportVisualisation(sourceRoot, destinationRoot, relative);
                    } catch (URISyntaxException e) {
                        System.out.println("Could not import visualisation under " + path.toString());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void ImportVisualisation(Path sourceRoot, Path destinationRoot, Path path) throws IOException, URISyntaxException {
        Path sourcePath = sourceRoot.resolve(path);
        Path destinationPath = destinationRoot.resolve(path);
        System.out.println("destinationPath = " + destinationPath);
        Path destinationContent = destinationPath.resolve("content");
        System.out.println("destinationContent = " + destinationContent);
        String visualisationCode = destinationPath.getFileName().toString();
        System.out.println("visualisationCode = " + visualisationCode);
        String outputJsonPath = destinationPath.resolve("data.json").toString();
        System.out.println("outputJsonPath = " + outputJsonPath);

        // delete the directory if it already exists.
        if (Files.isDirectory(destinationPath)) {
            FileUtils.deleteDirectory(destinationPath.toFile());
        }

        if (!Files.isDirectory(destinationPath)) {
            // create a directory in the destination
            Files.createDirectories(destinationPath);
        }

        // copy the source files to the content directory of the
        FileUtils.copyDirectory(sourcePath.toFile(), destinationContent.toFile());

        // create a json object with the directory name as the code and title
        Visualisation visualisation = new Visualisation();
        visualisation.setUid(visualisationCode);
        visualisation.setDescription(new PageDescription());
        visualisation.getDescription().setTitle(visualisationCode);


        // read all HTML pages in the directory and populate json
        visualisation.setFilenames(DataVisualisationZip.extractHtmlFilenames.apply(destinationContent, destinationContent));

        String uri = "/visualisations/" + visualisationCode + "/"; //+ visualisation.getIndexPage();
        visualisation.setUri(new URI(uri));
        System.out.println("uri = " + uri);

        // persist the json file
        FileUtils.write(new File(outputJsonPath), ContentUtil.serialise(visualisation));
    }

}
