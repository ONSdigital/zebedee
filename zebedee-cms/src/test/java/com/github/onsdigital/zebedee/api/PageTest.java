package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.page.MockPageUpdateHook;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.ApiDatasetLandingPage;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.commons.fileupload.FileUploadException;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PageTest extends ZebedeeAPIBaseTestCase {

    MockPageUpdateHook pageCreationHook = new MockPageUpdateHook();
    MockPageUpdateHook pageDeletionHook = new MockPageUpdateHook();

    @Test
    public void testPage_createPage() throws FileUploadException, ZebedeeException, IOException {

        ApiDatasetLandingPage requestPage = new ApiDatasetLandingPage();
        String bodyContent = ContentUtil.serialise(requestPage);

        // Given a page instance with mock dependencies
        ZebedeeCmsService zebedeeCmsService = mock(ZebedeeCmsService.class);
        Zebedee zebedee = mock(Zebedee.class);
        Collections collections = mock(Collections.class);
        com.github.onsdigital.zebedee.model.Collection collection =
                mock(com.github.onsdigital.zebedee.model.Collection.class);

        when(mockRequest.getParameter("uri")).thenReturn("/");
        when(collection.getPath()).thenReturn(Paths.get("/"));
        when(zebedeeCmsService.getZebedee()).thenReturn(zebedee);
        when(zebedeeCmsService.getSession(mockRequest)).thenReturn(session);
        when(zebedeeCmsService.getCollection(mockRequest)).thenReturn(collection);
        when(zebedee.getCollections()).thenReturn(collections);

        when(mockRequest.getInputStream()).thenReturn(
                new DelegatingServletInputStream(
                        new ByteArrayInputStream(bodyContent.getBytes())));

        Page page = new Page(zebedeeCmsService, pageCreationHook, pageDeletionHook);

        // When createPage is called
        page.createPage(mockRequest, mockResponse);

        // Then the page update hook is called
        assertTrue(pageCreationHook.wasOnPageUpdatedCalled());
    }

    @Test
    public void testPage_deletePage() throws ZebedeeException, IOException {

        // Given a page instance with mock dependencies
        ZebedeeCmsService zebedeeCmsService = mock(ZebedeeCmsService.class);
        Zebedee zebedee = mock(Zebedee.class);
        Collections collections = mock(Collections.class);
        CollectionReader collectionReader = mock(CollectionReader.class);
        com.github.onsdigital.zebedee.model.Collection collection =
                mock(com.github.onsdigital.zebedee.model.Collection.class);

        ApiDatasetLandingPage pageToDelete = new ApiDatasetLandingPage();
        String uri = "/";

        when(mockRequest.getParameter("uri")).thenReturn(uri);
        when(collection.getPath()).thenReturn(Paths.get(uri));
        when(collectionReader.getContent(uri)).thenReturn(pageToDelete);
        when(zebedeeCmsService.getZebedee()).thenReturn(zebedee);
        when(zebedeeCmsService.getSession(mockRequest)).thenReturn(session);
        when(zebedeeCmsService.getCollection(mockRequest)).thenReturn(collection);
        when(zebedeeCmsService.getZebedeeCollectionReader(collection, session)).thenReturn(collectionReader);
        when(zebedee.getCollections()).thenReturn(collections);

        Page page = new Page(zebedeeCmsService, pageCreationHook, pageDeletionHook);

        // When delete is called
        page.deletePage(mockRequest, mockResponse);

        // Then the page update hook is called
        assertTrue(pageDeletionHook.wasOnPageUpdatedCalled());
    }

    @Override
    protected void customSetUp() throws Exception { }

    @Override
    protected Object getAPIName() {
        return Page.class.getSimpleName();
    }
}
