package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

public interface CollectionReader {

    Page getContent(String path) throws ZebedeeException, IOException;

    Resource getResource(String path) throws ZebedeeException, IOException;

    Map<URI, ContentNode> getChildren(String path) throws ZebedeeException, IOException;

    Map<URI, ContentNode> getParents(String path) throws ZebedeeException, IOException;

    Page getLatestContent(String path) throws ZebedeeException, IOException;
}
