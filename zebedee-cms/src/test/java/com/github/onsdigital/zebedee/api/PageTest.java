package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.TestUtils;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.page.MockPageUpdateHook;
import com.github.onsdigital.zebedee.content.page.PageUpdateHook;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.ApiDatasetLandingPage;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.ForbiddenException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PageTest extends ZebedeeAPIBaseTestCase {

    private final MockPageUpdateHook pageCreationHook = new MockPageUpdateHook();
    private final MockPageUpdateHook pageDeletionHook = new MockPageUpdateHook();

    private final ZebedeeCmsService zebedeeCmsService = mock(ZebedeeCmsService.class);
    private final Zebedee zebedee = mock(Zebedee.class);
    private final Collections collections = mock(Collections.class);
    private final com.github.onsdigital.zebedee.model.Collection collection =
            mock(com.github.onsdigital.zebedee.model.Collection.class);
    private final CollectionReader collectionReader = mock(CollectionReader.class);

    private static final String uri = "/";
    private static final ApiDatasetLandingPage page = new ApiDatasetLandingPage();
    private static final byte[] serialisedPageBytes = ContentUtil.serialise(page).getBytes();

    @BeforeClass
    public static void setup() {
        TestUtils.initReaderConfig();
    }

    @AfterClass
    public static void tearDown() {
        TestUtils.clearReaderConfig();
    }

    @Override
    protected void customSetUp() throws Exception {

        when(mockRequest.getInputStream()).thenReturn(
                new DelegatingServletInputStream(
                        new ByteArrayInputStream(serialisedPageBytes)));

        when(collection.getPath()).thenReturn(Paths.get(uri));
        when(zebedeeCmsService.getZebedee()).thenReturn(zebedee);
        when(zebedeeCmsService.getZebedeeCollectionReader(collection, session)).thenReturn(collectionReader);
        when(zebedee.getCollections()).thenReturn(collections);
        when(zebedeeCmsService.getCollection(mockRequest)).thenReturn(collection);

        CollectionDescription collectionDescription = new CollectionDescription();
        collectionDescription.setId("123");
        when(collection.getDescription()).thenReturn(collectionDescription);
    }

    @Test
    public void testPage_createPage() throws ZebedeeException, IOException, FileUploadException {

        // Given a page instance with mock dependencies
        when(mockRequest.getParameter("uri")).thenReturn(uri);
        when(zebedeeCmsService.getSession(mockRequest)).thenReturn(session);
        when(zebedeeCmsService.getCollection(mockRequest)).thenReturn(collection);

        Page page = new Page(zebedeeCmsService, pageCreationHook, pageDeletionHook, true);

        // When createPage is called
        page.createPage(mockRequest, mockResponse);

        // Then the page update hook is called
        assertTrue(pageCreationHook.wasOnPageUpdatedCalled());

        // Then collections.createContent is called
        verify(collections, times(1)).createContent(any(), any(), any(), any(), any(), any(), anyBoolean());
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_CREATED);
    }

    @Test
    public void testPage_createPage_uriNotFound() {

        // Given a null uri
        when(mockRequest.getParameter("uri")).thenReturn(null);

        Page page = new Page(zebedeeCmsService, pageCreationHook, pageDeletionHook, true);

        // When createPage is called
        page.createPage(mockRequest, mockResponse);

        // Then the http response code is set as expected
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testPage_createPage_sessionNotFound() throws ZebedeeException {

        // Given a page instance where sessions cannot be found
        when(mockRequest.getParameter("uri")).thenReturn(uri);
        when(zebedeeCmsService.getSession(mockRequest)).thenThrow(new UnauthorizedException(""));

        Page page = new Page(zebedeeCmsService, pageCreationHook, pageDeletionHook, true);

        // When createPage is called
        page.createPage(mockRequest, mockResponse);

        // Then the http response code is set as expected
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void testPage_createPage_collectionNotFound() throws ZebedeeException {

        // Given a page instance where collections cannot be found
        when(mockRequest.getParameter("uri")).thenReturn(uri);
        when(zebedeeCmsService.getSession(mockRequest)).thenReturn(session);
        when(zebedeeCmsService.getCollection(mockRequest)).thenThrow(new NotFoundException(""));

        Page page = new Page(zebedeeCmsService, pageCreationHook, pageDeletionHook, true);

        // When createPage is called
        page.createPage(mockRequest, mockResponse);

        // Then the http response code is set as expected
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testPage_createPage_pageHookException() throws ZebedeeException, IOException {
        // Given a mock page hook that throws an exception
        PageUpdateHook mockPageHook = mock(PageUpdateHook.class);
        doThrow(new RuntimeException()).when(mockPageHook).onPageUpdated(any(), anyString());

        when(mockRequest.getParameter("uri")).thenReturn(uri);
        when(zebedeeCmsService.getSession(mockRequest)).thenReturn(session);
        when(zebedeeCmsService.getCollection(mockRequest)).thenReturn(collection);

        Page page = new Page(zebedeeCmsService, mockPageHook, pageDeletionHook, true);

        // When createPage is called
        page.createPage(mockRequest, mockResponse);

        // Then the http response code is set as expected
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testPage_createPage_zebedeeCreatePageException() throws ZebedeeException, IOException, FileUploadException {

        // Given a mock collections that throws an exception on createContent
        doThrow(new ConflictException(""))
                .when(collections).createContent(any(), any(), any(), any(), any(), any(), anyBoolean());

        when(mockRequest.getParameter("uri")).thenReturn(uri);
        when(zebedeeCmsService.getSession(mockRequest)).thenReturn(session);
        when(zebedeeCmsService.getCollection(mockRequest)).thenReturn(collection);

        Page page = new Page(zebedeeCmsService, pageCreationHook, pageDeletionHook, true);

        // When createPage is called
        page.createPage(mockRequest, mockResponse);

        // Then the http response code is set as expected
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_CONFLICT);
    }

    @Test
    public void testPage_deletePage_getContentException() throws ZebedeeException, IOException {

        // Given a mock collection reader that throws an exception on getContent
        doThrow(new ForbiddenException(""))
                .when(collectionReader).getContent(anyString());

        when(mockRequest.getParameter("uri")).thenReturn(uri);
        when(zebedeeCmsService.getSession(mockRequest)).thenReturn(session);
        when(zebedeeCmsService.getCollection(mockRequest)).thenReturn(collection);

        Page page = new Page(zebedeeCmsService, pageCreationHook, pageDeletionHook, true);

        // When delete is called
        page.deletePage(mockRequest, mockResponse);

        // Then the http response code is set as expected
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testPage_deletePage_deleteContentException() throws ZebedeeException, IOException {

        // Given a mock collections that throws an exception on deleteContent
        doThrow(new ForbiddenException(""))
                .when(collections).deleteContent(any(), any(), any());

        when(mockRequest.getParameter("uri")).thenReturn(uri);
        when(zebedeeCmsService.getSession(mockRequest)).thenReturn(session);
        when(zebedeeCmsService.getCollection(mockRequest)).thenReturn(collection);

        Page page = new Page(zebedeeCmsService, pageCreationHook, pageDeletionHook, true);

        // When delete is called
        page.deletePage(mockRequest, mockResponse);

        // Then the http response code is set as expected
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void testPage_deletePage() throws ZebedeeException, IOException {

        // Given a page instance with mock dependencies
        when(mockRequest.getParameter("uri")).thenReturn(uri);
        when(collectionReader.getContent(uri)).thenReturn(page);
        when(zebedeeCmsService.getSession(mockRequest)).thenReturn(session);
        when(zebedeeCmsService.getCollection(mockRequest)).thenReturn(collection);

        Page page = new Page(zebedeeCmsService, pageCreationHook, pageDeletionHook, true);

        // When delete is called
        page.deletePage(mockRequest, mockResponse);

        // Then the page update hook is called
        assertTrue(pageDeletionHook.wasOnPageUpdatedCalled());

        // Then collections.createContent is called
        verify(collections, times(1)).deleteContent(any(), any(), any());
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testPage_deletePage_uriNotFound() {

        // Given an empty uri in the request
        when(mockRequest.getParameter("uri")).thenReturn("");

        Page page = new Page(zebedeeCmsService, pageCreationHook, pageDeletionHook, true);

        // When delete is called
        page.deletePage(mockRequest, mockResponse);

        // Then the http response code is set as expected
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testPage_deletePage_sessionNotFound() throws ZebedeeException {

        // Given a mock zebedeeCmsService that throws an exception on getSession
        when(mockRequest.getParameter("uri")).thenReturn(uri);
        when(zebedeeCmsService.getZebedee()).thenReturn(zebedee);
        when(zebedeeCmsService.getSession(mockRequest)).thenThrow(new UnauthorizedException(""));
        when(zebedee.getCollections()).thenReturn(collections);

        Page page = new Page(zebedeeCmsService, pageCreationHook, pageDeletionHook, true);

        // When delete is called
        page.deletePage(mockRequest, mockResponse);

        // Then the http response code is set as expected
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void testPage_deletePage_collectionNotFound() throws ZebedeeException, IOException {

        // Given a mock zebedeeCmsService that throws an exception on getCollection
        when(mockRequest.getParameter("uri")).thenReturn(uri);
        when(collectionReader.getContent(uri)).thenReturn(page);
        when(zebedeeCmsService.getSession(mockRequest)).thenReturn(session);
        when(zebedeeCmsService.getCollection(mockRequest)).thenThrow(new NotFoundException(""));

        Page page = new Page(zebedeeCmsService, pageCreationHook, pageDeletionHook, true);

        // When delete is called
        page.deletePage(mockRequest, mockResponse);

        // Then the http response code is set as expected
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testPage_deletePage_pageHookException() throws ZebedeeException, IOException {

        // Given a mock page hook that throws an exception
        PageUpdateHook mockPageHook = mock(PageUpdateHook.class);
        doThrow(new RuntimeException()).when(mockPageHook).onPageUpdated(any(), anyString());

        when(mockRequest.getParameter("uri")).thenReturn(uri);
        when(zebedeeCmsService.getSession(mockRequest)).thenReturn(session);
        when(zebedeeCmsService.getCollection(mockRequest)).thenReturn(collection);

        Page page = new Page(zebedeeCmsService, pageCreationHook, mockPageHook, true);

        // When deletePage is called
        page.deletePage(mockRequest, mockResponse);

        // Then the http response code is set as expected
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testPage_deletePage_doesNotExist() throws ZebedeeException, IOException {

        // Given a mock collection reader that throws a not found exception
        when(collectionReader.getContent(uri)).thenThrow(new NotFoundException("page not found"));

        when(mockRequest.getParameter("uri")).thenReturn(uri);
        when(zebedeeCmsService.getSession(mockRequest)).thenReturn(session);

        Page page = new Page(zebedeeCmsService, pageCreationHook, pageDeletionHook, true);

        // When delete is called
        page.deletePage(mockRequest, mockResponse);

        // Then no exceptions are thrown and no response status set, allowing the call to return as normal
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testTrimZebedeeSuffix() {

        // Given a uri
        String expectedUri = "/some/uri";

        // When trimZebedeeFileSuffix is called with a URI containing the suffix
        String uri = Page.trimZebedeeFileSuffix(expectedUri + Page.zebedeeFileSuffix);

        // Then the returned URI does not have the suffix
        assertEquals(expectedUri, uri);
    }

    @Override
    protected Object getAPIName() {
        return Page.class.getSimpleName();
    }
}
