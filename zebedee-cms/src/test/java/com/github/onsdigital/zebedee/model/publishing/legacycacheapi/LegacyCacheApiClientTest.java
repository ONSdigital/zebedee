package com.github.onsdigital.zebedee.model.publishing.legacycacheapi;

import com.github.davidcarboni.httpino.Host;
import com.github.davidcarboni.httpino.Http;
import com.github.davidcarboni.httpino.Response;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.publishing.WebsiteResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class LegacyCacheApiClientTest {
    @Mock
    private Collection collection;
    private AutoCloseable mockitoAnnotations;
    private List<LegacyCacheApiPayload> payloads;
    private Http httpMock;
    private Host host;

    @Before
    public void setup() {
        mockitoAnnotations = MockitoAnnotations.openMocks(this);

        CollectionDescription cdmock = mock(CollectionDescription.class);
        when(collection.getDescription()).thenReturn(cdmock);

        LegacyCacheApiPayload legacyCacheApiPayload = new LegacyCacheApiPayload(collection.getDescription().getId(), null, new Date());
        payloads = new ArrayList<>();
        payloads.add(legacyCacheApiPayload);

        httpMock = mock(Http.class);

        host = new Host("http://localhost:29100");
    }

    @After
    public void tearDown() throws Exception {
        mockitoAnnotations.close();
    }

    @Test
    public void sendNotificationPUTNeverCalledWhenPayloadsIsEmptyTest() throws IOException {
        payloads.clear();

        LegacyCacheApiClient.sendPayloads(httpMock, host, payloads);

        verify(httpMock, never()).put(any(), any(), any(), any());
    }

    @Test
    public void sendNotificationThrowsExceptionWhenURIIsEmptyTest() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> LegacyCacheApiClient.sendPayloads(httpMock, host, payloads));

        verify(httpMock, never()).put(any(), any(), any(), any());
    }

    @Test
    public void sendNotificationPUTCalledWhenPayloadIsSetTest() throws IOException {
        payloads.get(0).uriToUpdate = "/economy/inflationandprices/bulletins/latest";

        WebsiteResponse websiteResponse = new WebsiteResponse();
        websiteResponse.setMessage("OK");
        Response<Object> response = new Response<>(200, "OK", websiteResponse);

        when(httpMock.put(any(), any(), any(), any())).thenReturn(response);

        LegacyCacheApiClient.sendPayloads(httpMock, host, payloads);

        verify(httpMock, times(1)).put(any(), any(), any(), any());
    }
}
