package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ContentReader {
    Page getContent(String path) throws ZebedeeException, IOException;

    Page getLatestContent(String path) throws ZebedeeException, IOException;

    Resource getResource(String path) throws ZebedeeException, IOException;

    long getContentLength(String path) throws ZebedeeException, IOException;

    Map<URI, ContentNode> getChildren(String path) throws ZebedeeException, IOException;

    DirectoryStream<Path> getDirectoryStream(String path) throws BadRequestException, IOException;

    DirectoryStream<Path> getDirectoryStream(String path, String filter) throws BadRequestException, IOException;

    Map<URI, ContentNode> getParents(String path) throws ZebedeeException, IOException;

    /*Getters * Setters */
    Path getRootFolder();

    ContentLanguage getLanguage();

    void setLanguage(ContentLanguage language);

    List<String> listUris();

    List<Path> listTimeSeriesDirectories();
}
