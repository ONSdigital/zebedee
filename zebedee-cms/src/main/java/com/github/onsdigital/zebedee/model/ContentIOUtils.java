package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utils for dealing with a ContentReader and ContentWriter world
 */
public class ContentIOUtils {

    /**
     * Recursively copy all content under a ContentReader to a stream defined by a ContentWriter
     *
     * @param input  a ContentReader
     * @param output a ContentWriter
     * @throws ZebedeeException
     * @throws IOException
     */
    public static void copy(ContentReader input, ContentWriter output) throws ZebedeeException, IOException {
        copy(input, output, "");
    }

    /**
     * Recursively copy content of a subdirectory using ContentReader and ContentWriter
     *
     * @param input     an input ContentReader
     * @param output    an output ContentWriter
     * @param directory the subdirectory to copy
     * @throws ZebedeeException
     * @throws IOException
     */
    private static void copy(ContentReader input, ContentWriter output, String directory) throws ZebedeeException, IOException {
        try (DirectoryStream<Path> directoryStream = input.getDirectoryStream(directory)) {
            for (Path path : directoryStream) {

                String uri = input.getRootFolder().relativize(path).toString();
                if (Files.isDirectory(path)) {

                    // recursive copy folder contents
                    copy(input, output, uri);
                } else {

                    // copy
                    try (
                            Resource resource = input.getResource(uri);
                            InputStream inputStream = resource.getData();
                            OutputStream outputStream = output.getOutputStream(uri)
                    ) {
                        IOUtils.copy(inputStream, outputStream);
                    }
                }
            }
        }
    }
}
