package com.github.onsdigital.zebedee.reader.api;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.reader.data.filter.DataFilter;
import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;
import com.github.onsdigital.zebedee.reader.util.ContentNodeComparator;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.NoSuchFileException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logTrace;
import static com.github.onsdigital.zebedee.util.URIUtils.getLastSegment;
import static com.github.onsdigital.zebedee.util.URIUtils.removeLastSegment;

/**
 * Created by bren on 31/07/15.
 * <p>
 * This class checks to see if zebedee cms is running to authorize collection views via registered AuthorisationHandler, if not serves published content
 */
public class ReadRequestHandler {

    private final static String LATEST = "latest";
    private ZebedeeReader reader;

    public ReadRequestHandler() {
        this(null);
    }

    public ReadRequestHandler(ContentLanguage language) {
        this.reader = new ZebedeeReader(language);
    }

    /**
     * Finds requested content , if a collection is required handles authorisation
     * <p>
     * If requested uri ends in "latest" it will return latest edition of bulletin or article content, throws BadRequestException given uri is not a bulletin or article content
     *
     * @param request
     * @param dataFilter
     * @return Content
     * @throws ZebedeeException
     * @throws IOException
     */
    public Content findContent(HttpServletRequest request, DataFilter dataFilter) throws ZebedeeException, IOException {
        String uri = extractUri(request);
        return find(request, dataFilter, uri);
    }

    public Content find(HttpServletRequest request, DataFilter dataFilter, String uri) throws IOException, ZebedeeException {
        String collectionId = getCollectionId(request);
        String lastSegment = getLastSegment(uri);
        logTrace("finding requested content").uri(uri).collectionId(collectionId).log();
        if (LATEST.equalsIgnoreCase(lastSegment)) {
            return getLatestContent(request, collectionId, dataFilter, removeLastSegment(uri));
        } else {
            return getContent(request, collectionId, dataFilter, uri);
        }
    }


    private Content getLatestContent(HttpServletRequest request, String collectionId, DataFilter dataFilter, String uri) throws IOException, ZebedeeException {

        if (collectionId != null) {
            try {
                String sessionId = RequestUtils.getSessionId(request);
                return reader.getLatestCollectionContent(collectionId, sessionId, uri, dataFilter);
            } catch (NotFoundException | NoSuchFileException e) {
                logTrace("Could not find resource in collection. Will try published content")
                        .addParameter("resourceURI", uri)
                        .collectionId(collectionId)
                        .log();
            }
        }

        return reader.getLatestPublishedContent(uri, dataFilter);
    }

    public Content getContent(HttpServletRequest request, String collectionId, DataFilter dataFilter, String uri) throws IOException, ZebedeeException {
        if (collectionId != null) {
            try {
                String sessionId = RequestUtils.getSessionId(request);
                return reader.getCollectionContent(collectionId, sessionId, uri, dataFilter);
            } catch (NotFoundException e) {
                logTrace("Could not find resource in collection. Will try published content")
                        .addParameter("uri", uri)
                        .collectionId(collectionId)
                        .log();
            }
        }

        return reader.getPublishedContent(uri, dataFilter);
    }

    /**
     * Finds requested resource , if a collection resource is required handles authorisation
     *
     * @param request
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    public Resource findResource(HttpServletRequest request) throws ZebedeeException, IOException {
        String uri = URLDecoder.decode(extractUri(request), "UTF-8");
        String collectionId = getCollectionId(request);
        if (collectionId != null) {
            try {
                String sessionId = RequestUtils.getSessionId(request);
                return reader.getCollectionResource(collectionId, sessionId, uri);
            } catch (NotFoundException e) {
                logTrace("Could not find resource under collection, trying published content")
                        .addParameter("resourceUri", uri)
                        .collectionId(collectionId)
                        .log();
            }
        }
        return reader.getPublishedResource(uri);
    }

    /**
     * Finds requested resource , if a collection resource is required handles authorisation
     *
     * @param request
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    public long getContentLength(HttpServletRequest request) throws ZebedeeException, IOException {
        String uri = extractUri(request);
        String collectionId = getCollectionId(request);
        if (collectionId != null) {
            try {
                String sessionId = RequestUtils.getSessionId(request);
                return reader.getCollectionContentLength(collectionId, sessionId, uri);
            } catch (NotFoundException e) {
                logTrace("Could not find resource in collection. Will try published content")
                        .addParameter("uri", uri)
                        .collectionId(collectionId)
                        .log();
            }
        }
        return reader.getPublishedContentLength(uri);
    }

    public Collection<ContentNode> getTaxonomy(HttpServletRequest request, int depth) throws ZebedeeException, IOException {
        String collectionId = getCollectionId(request);
        String sessionId = RequestUtils.getSessionId(request);
        return getTaxonomy(collectionId, sessionId, "/", depth);

    }

    public Collection<ContentNode> getParents(HttpServletRequest request) throws IOException, ZebedeeException {
        String uri = extractUri(request);
        String collectionId = getCollectionId(request);
        String sessionId = RequestUtils.getSessionId(request);
        return resolveParents(collectionId, sessionId, uri);
    }

    private Collection<ContentNode> resolveParents(String collectionId, String sessionId, String uri) throws ZebedeeException, IOException {
        Map<URI, ContentNode> nodes = reader.getPublishedContentParents(uri);
        overlayCollectionParents(nodes, collectionId, sessionId, uri);
        nodes = new TreeMap<>(nodes);//sort by uri, sorts by uris, child uris naturally comes after parent uris
        return nodes.values();
    }

    private Collection<ContentNode> getTaxonomy(String collectionId, String sessionId, String uri, int depth) throws ZebedeeException, IOException {
        if (depth == 0) {
            return Collections.emptySet();
        }
        Map<URI, ContentNode> nodes = reader.getPublishedContentChildren(uri);
        overlayCollections(nodes, collectionId, sessionId, uri);
        nodes = sortMapByContentTitle(nodes);
        depth--;
        getTaxonomy(nodes, collectionId, sessionId, depth);
        return nodes.values();
    }

    private void getTaxonomy(Map<URI, ContentNode> nodes, String collectionId, String sessionId, int depth) throws ZebedeeException, IOException {
        if (depth == 0) {
            return;
        }
        Collection<ContentNode> nodeList = nodes.values();
        for (Iterator<ContentNode> iterator = nodeList.iterator(); iterator.hasNext(); ) {
            ContentNode next = iterator.next();
            if (PageType.taxonomy_landing_page.equals(next.getType()) == false) {
                continue;
            }
            next.setChildren(getTaxonomy(collectionId, sessionId, next.getUri().toString(), depth));
        }
    }

    private void overlayCollections(Map<URI, ContentNode> nodes, String collectionId, String sessionId, String uri) throws ZebedeeException, IOException {
        if (collectionId == null) {
            return;
        }
        Map<URI, ContentNode> collectionChildren = reader.getCollectionContentChildren(collectionId, sessionId, uri);
        for (Map.Entry<URI, ContentNode> collectionEntry : collectionChildren.entrySet()) {
            ContentNode publishedNode = nodes.get(collectionEntry.getKey());
            ContentNode collectionNode = collectionEntry.getValue();
            //do not overlay non-content directory to published content
            if (publishedNode == null || collectionNode.getType() != null) {
                nodes.put(collectionNode.getUri(), collectionNode);
            }
        }
    }

    private void overlayCollectionParents(Map<URI, ContentNode> nodes, String collectionId, String sessionId, String uri) throws ZebedeeException, IOException {
        if (collectionId == null) {
            return;
        }
        Map<URI, ContentNode> collectionContentParents = reader.getCollectionContentParents(collectionId, sessionId, uri);
        nodes.putAll(collectionContentParents);
    }

    /*By default tries to read collection id from cookies named collection. If not found falls back to reading from uri.*/
    private String getCollectionId(HttpServletRequest request) {
        return RequestUtils.getCollectionId(request);
    }

    private String extractUri(HttpServletRequest request) throws BadRequestException {
        String uri = request.getParameter("uri");
        if (StringUtils.isEmpty(uri)) {
            throw new BadRequestException("Please specify uri");
        }

        return uri;
    }

    /**
     * Sorts given map by content node rather than map key
     *
     * @param nodes
     * @return
     */
    private Map<URI, ContentNode> sortMapByContentTitle(Map<URI, ContentNode> nodes) {
        TreeMap<URI, ContentNode> sortedMap = new TreeMap<>(new ContentNodeComparator(nodes, false));
        sortedMap.putAll(nodes);
        return sortedMap;
    }

}
