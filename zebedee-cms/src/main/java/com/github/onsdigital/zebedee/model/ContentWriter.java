package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.github.onsdigital.zebedee.util.URIUtils.removeLeadingSlash;
import static java.nio.file.Files.isDirectory;

/**
 * Basic abstraction for writing content to file.
 */
public class ContentWriter {

    private final Path ROOT_FOLDER;

    /**
     * Create a new instance of ContentWriter to write content from the given root folder.
     *
     * @param rootFolder
     */
    public ContentWriter(Path rootFolder) {
        if (rootFolder == null) {
            throw new NullPointerException("Root folder can not be null");
        }
        this.ROOT_FOLDER = rootFolder;
    }

    public void writeObject(Object object, String uri) throws IOException, BadRequestException, URISyntaxException {
        try(InputStream stream = IOUtils.toInputStream(ContentUtil.serialise(object))) {
            System.out.println("SAVING FILE TO DISK");
            System.out.println(uri);
            write(stream,uri);


            File file = new File(uri);

            if (!file.exists()){
                System.out.println("THE FILE IS STILL MISSING");
            }
        }
    }

    /**
     * Write the given input stream to the given URI.
     *
     * @param input
     * @param uri
     * @throws BadRequestException
     * @throws IOException
     */
    public void write(InputStream input, String uri) throws IOException, BadRequestException {
        try (OutputStream output = getOutputStream(uri)) {
            org.apache.commons.io.IOUtils.copy(input, output);
        }
    }

    /**
     * Get an output stream for the given URI.
     *
     * @param uri
     * @return
     * @throws IOException
     * @throws BadRequestException
     */
    public OutputStream getOutputStream(String uri) throws IOException, BadRequestException {
        Path path = resolvePath(uri);
        assertNotDirectory(path);
        return FileUtils.openOutputStream(path.toFile());
    }

    protected void assertNotDirectory(Path path) throws BadRequestException {
        if (isDirectory(path)) {
            throw new BadRequestException("Requested path is a directory");
        }
    }

    protected Path resolvePath(String path) {
        if (path == null) {
            throw new NullPointerException("Path can not be null");
        }
        return getRootFolder().resolve(removeLeadingSlash(path));
    }

    protected Path getRootFolder() {
        return ROOT_FOLDER;
    }

}
