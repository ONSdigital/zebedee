package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utilities related to file compression.
 */
public class ZipUtils {

    /**
     * Unzip the given file into the given destination.
     *
     * @param zipFile
     * @param destination
     * @throws IOException
     */
    public static void unzip(final File zipFile, final String destination) throws IOException {
        unzip(new FileInputStream(zipFile), destination);
    }

    public static void unzip(final InputStream inputStream, final String destination) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null && !zipEntry.isDirectory()) {
                String name = zipEntry.getName();
                File file = new File(destination + File.separator + name);
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }

                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    IOUtils.copy(zipInputStream, fileOutputStream);
                }
            }
        }
    }

    /**
     * Unzip the given input stream into the given content writer.
     *
     * @param inputStream
     * @param uri
     * @param contentWriter
     * @throws IOException
     * @throws BadRequestException
     */
    public static List<String> unzip(final InputStream inputStream, String uri, final ContentWriter contentWriter) throws IOException, BadRequestException {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;

            List<String> unzipped = new ArrayList<>();

            while ((zipEntry = zipInputStream.getNextEntry()) != null && !zipEntry.isDirectory()) {
                String name = zipEntry.getName();
                String fileUri = uri + "/" + name;
                contentWriter.write(zipInputStream, fileUri);

                unzipped.add(fileUri);
            }

            return unzipped;
        }
    }

    /**
     * Zip the given folder into the given zip file.
     *
     * @param folder
     * @param zipFile
     * @throws IOException
     */
    public static int zipFolder(final File folder, final File zipFile, Function<String, Boolean>... filters) throws IOException {
        return zipFolder(folder, new FileOutputStream(zipFile), filters);
    }

    public static int zipFolder(final File folder, final OutputStream outputStream, Function<String, Boolean>... filters) throws IOException {
        try (ZipOutputStream zipOutputStream = getZipOutputStream(outputStream)) {
            return zipFolder(folder, zipOutputStream, folder.getPath().length() + 1, filters);
        }
    }

    private static int zipFolder(final File folder, final ZipOutputStream zipOutputStream, final int prefixLength, Function<String, Boolean>... filters)
            throws IOException {
        int filesAdded = 0;
        for (final File file : folder.listFiles()) {
            if (file.isFile() && !shouldBeFiltered(filters, file.toString())) {
                final ZipEntry zipEntry = new ZipEntry(file.getPath().substring(prefixLength));
                zipOutputStream.putNextEntry(zipEntry);
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    IOUtils.copy(inputStream, zipOutputStream);
                }
                zipOutputStream.closeEntry();
                filesAdded++;
            } else if (file.isDirectory()) {
                filesAdded += zipFolder(file, zipOutputStream, prefixLength, filters);
            }
        }
        return filesAdded;
    }

    /**
     * If any of the given filters return true, the uri should be filtered
     *
     * @param filters
     * @param uri
     * @return
     */
    private static boolean shouldBeFiltered(Function<String, Boolean>[] filters, String uri) {
        for (Function<String, Boolean> filter : filters) {
            if (filter.apply(uri))
                return true;
        }
        return false;
    }

    private static ZipOutputStream getZipOutputStream(OutputStream outputStream) {
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        zipOutputStream.setLevel(5); // minimal compression
        return zipOutputStream;
    }


    public static int zipFolderWithEncryption(
            final ContentReader contentReader,
            final ContentWriter contentWriter,
            String folderPath,
            String saveUri,
            Function<String, Boolean>... filters) throws IOException, ZebedeeException {
        try (ZipOutputStream zipOutputStream = getZipOutputStream(contentWriter.getOutputStream(saveUri))) {
            return zipFolderWithEncryption(contentReader, folderPath, zipOutputStream, folderPath.length() + 1, filters);
        } catch (BadRequestException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int zipFolderWithEncryption(
            final ContentReader contentReader,
            String folderUri,
            final ZipOutputStream zipOutputStream,
            final int prefixLength,
            Function<String, Boolean>... filters)
            throws IOException, ZebedeeException {

        File folder = Paths.get(folderUri).toFile();
        int filesAdded = 0;

        for (final File file : folder.listFiles()) {
            String fileUri = contentReader.getRootFolder().relativize(file.toPath()).toString();
            if (file.isFile() && !shouldBeFiltered(filters, file.toString())) {
                final ZipEntry zipEntry = new ZipEntry(file.getPath().substring(prefixLength));
                zipOutputStream.putNextEntry(zipEntry);
                try (
                        Resource resource = contentReader.getResource(fileUri);
                        InputStream inputStream = resource.getData()
                ) {
                    IOUtils.copy(inputStream, zipOutputStream);
                }
                zipOutputStream.closeEntry();
                filesAdded++;
            } else if (file.isDirectory()) {
                filesAdded += zipFolderWithEncryption(contentReader, contentReader.getRootFolder().resolve(fileUri).toString(), zipOutputStream, prefixLength);
            }
        }
        return filesAdded;
    }
}
