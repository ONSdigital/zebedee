package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.commons.io.FileUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class ContentMover {

    public static void moveContent(String[] args) throws InterruptedException, BadRequestException, NotFoundException, IOException {
        // args[1] - source data directory
        // args[2] - destination directory to save the updated content (can be a collection or master)
        // args[3] - source URI
        // args[4] - destination URI

        Path source = Paths.get(args[1]);
        Path destination = Paths.get(args[2]);
        String sourceUri = args[3];
        String destinationUri = args[4];

        moveContent(source, destination, sourceUri, destinationUri);
    }

    private static void moveContent(Path source, Path destination, String sourceUri, String destinationUri) throws IOException {

        // copy the original folder to the new URI in the destination
        Path sourceDirectory = source.resolve(URIUtils.removeLeadingSlash(sourceUri));
        Path destinationDirectory = destination.resolve(URIUtils.removeLeadingSlash(destinationUri));

        System.out.println("Moving from: "  + sourceDirectory + " to: " + destinationDirectory);

        FileUtils.copyDirectory(sourceDirectory.toFile(), destinationDirectory.toFile());

        // delete the old directory on publishing and log a message to delete the directory on live
        System.out.println("Directory to delete: " + sourceUri);
        //FileUtils.deleteDirectory(sourceDirectory);

        String latestUri = Paths.get(sourceUri).getParent().resolve("latest").toString();

        System.out.println("Searching collection content for links for fix...." + destination);
        // do the same process for files in the collection in case they need links fixing
        List<Path> collectionJsonFiles = new DataJsonFinder().findJsonFiles(destination);
        Set<Path> collectionFilesToFixLinksIn = findJsonFilesWithLinksToFix(sourceDirectory, sourceUri, latestUri, collectionJsonFiles);
        FixLinksAndWriteBackToCollection(sourceUri, destinationUri, collectionFilesToFixLinksIn);

        System.out.println("Searching master content for links for fix....");
        // identify pages in master content with links to fix - search source path for old URI
        List<Path> masterJsonFiles = new DataJsonFinder().findJsonFiles(source);
        Set<Path> filesToFixLinksIn = findJsonFilesWithLinksToFix(sourceDirectory, sourceUri, latestUri, masterJsonFiles);
        FixLinksAndWriteToDestination(source, destination, sourceUri, destinationUri, filesToFixLinksIn);

        // publish the collection...
        // run the delete commands on the web servers and reindex search on publishing and web
    }

    private static void FixLinksAndWriteBackToCollection(String sourceUri, String destinationUri, Set<Path> collectionFilesToFixLinksIn) throws IOException {

        String sourceLatestUri = Paths.get(sourceUri).getParent().resolve("latest").toString();
        String destinationLatestUri = Paths.get(destinationUri).getParent().resolve("latest").toString();

        for (Path path : collectionFilesToFixLinksIn) {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            content = content.replaceAll(sourceUri, destinationUri);
            content = content.replaceAll(sourceLatestUri, destinationLatestUri);
            System.out.println("destinationFilePath = " + path);
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void FixLinksAndWriteToDestination(Path source, Path destination, String sourceUri, String destinationUri, Set<Path> filesToFixLinksIn) throws IOException {

        String sourceLatestUri = Paths.get(sourceUri).getParent().resolve("latest").toString();
        String destinationLatestUri = Paths.get(destinationUri).getParent().resolve("latest").toString();

        for (Path path : filesToFixLinksIn) {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            content = content.replaceAll(sourceUri, destinationUri);
            content = content.replaceAll(sourceLatestUri, destinationLatestUri);

            Path destinationFilePath = destination.resolve(source.relativize(path));
            System.out.println("destinationFilePath = " + destinationFilePath);

            Path parentDirectory = destinationFilePath.getParent();
            if (!Files.exists(parentDirectory))
                Files.createDirectories(parentDirectory);

            Files.write(destinationFilePath, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static Set<Path> findJsonFilesWithLinksToFix(Path sourceDirectory, String sourceUri, String latestUri, List<Path> jsonFiles) {
        Set<Path> filesToFixLinksIn = new HashSet<>();

        for (Path jsonPath : jsonFiles) {

            if (jsonPath.toString().contains(sourceDirectory.toString())){
                System.out.println("Skipping file as it live under the directory being moved :" + jsonPath);
                continue;
            }

            try {
                Scanner scanner = new Scanner(jsonPath.toFile());
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.contains(sourceUri) || line.contains(latestUri)) {

                        System.out.println("Fix links in file: " + jsonPath);
                        filesToFixLinksIn.add(jsonPath);
                        break;
                    }
                }
            } catch(FileNotFoundException e) {
                //handle this
            }
        }

        return filesToFixLinksIn;
    }

}
