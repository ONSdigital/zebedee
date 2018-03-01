package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.page.MockPageUpdateHook;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.ApiDatasetLandingPage;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.commons.fileupload.FileUploadException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PageTest extends ZebedeeAPIBaseTestCase {

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

        MockPageUpdateHook pageUpdateHook = new MockPageUpdateHook();

        Page page = new Page(zebedeeCmsService, pageUpdateHook);

        // When createPage is called
        page.createPage(mockRequest, mockResponse);

        // Then the page update hook is called
        Assert.assertTrue(pageUpdateHook.wasOnPageUpdatedCalled());
    }

    @Override
    protected void customSetUp() throws Exception { }

    @Override
    protected Object getAPIName() {
        return Page.class.getSimpleName();
    }
}
