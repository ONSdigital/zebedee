package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.httpino.*;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.PendingDelete;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.publishing.legacycacheapi.LegacyCacheApiClient;
import com.github.onsdigital.zebedee.model.publishing.legacycacheapi.LegacyCacheApiPayload;
import com.github.onsdigital.zebedee.util.EncryptionUtils;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicStatusLine;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PublishNotificationTest {
    private MockedStatic<Configuration> mockConfiguration;
    private AutoCloseable mockitoAnnotations;
    @Mock
    private Collection collection;
    @Mock
    private Collection collectionWithDeletes;
    private Host legacyCacheApiHost;
    private List<String> urisToUpdate;
    @Captor
    private ArgumentCaptor<Endpoint> endpointCapture;
    @Captor
    private ArgumentCaptor<LegacyCacheApiPayload> payloadCapture;
    @Captor
    private ArgumentCaptor<Class<Object>> classCapture;

    @Before
    public void setUp() throws IOException {
        mockitoAnnotations = MockitoAnnotations.openMocks(this);
        mockConfiguration = mockStatic(Configuration.class);

        urisToUpdate = new ArrayList<>();
        urisToUpdate.add("/economy/inflationandprices/articles/producerpriceinflation/latest");
        urisToUpdate.add("/economy/inflationandprices/articles/producerpriceinflation/october2022");
        urisToUpdate.add("/economy/inflationandprices/articles/producerpriceinflation/october2023");

        String legacyCacheApiUrl = "http://localhost:29100";
        legacyCacheApiHost = new Host(legacyCacheApiUrl);
        when(Configuration.getLegacyCacheApiUrl()).thenReturn(legacyCacheApiUrl);

        List<Host> websiteHosts = new ArrayList<>();
        websiteHosts.add(new Host("http://localhost:8080"));
        when(Configuration.getWebsiteHosts()).thenReturn(websiteHosts);

        Date publishDate = new Date(1609866000000L);
        Date clearCacheDateTime = new DateTime(publishDate).plusSeconds(Configuration.getSecondsToCacheAfterScheduledPublish()).toDate();

        CollectionDescription mockCollectionDescription = mock(CollectionDescription.class);
        CollectionDescription mockCollectionDescriptionWithDeletes = mock(CollectionDescription.class);
        when(mockCollectionDescription.getId()).thenReturn("cake-1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
        when(mockCollectionDescription.getPublishDate()).thenReturn(clearCacheDateTime);
        when(mockCollectionDescriptionWithDeletes.getId()).thenReturn("cake-1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
        when(mockCollectionDescriptionWithDeletes.getPublishDate()).thenReturn(clearCacheDateTime);

        when(collection.getDescription()).thenReturn(mockCollectionDescription);
        when(collection.reviewedUris()).thenReturn(urisToUpdate);

        PendingDelete pendingDelete = mock(PendingDelete.class);
        PendingDelete pendingDelete2 = mock(PendingDelete.class);
        ContentDetail contentDetail = mock(ContentDetail.class);
        ContentDetail contentDetail2 = mock(ContentDetail.class);

        when(pendingDelete.getRoot()).thenReturn(contentDetail);
        when(pendingDelete2.getRoot()).thenReturn(contentDetail2);
        when(contentDetail.getUri()).thenReturn("/economy/inflationandprices/bulletins/producerpriceinflation/october2022/data.json");
        when(contentDetail2.getUri()).thenReturn("/economy/inflationandprices/bulletins/producerpriceinflation/october2023/data.json");

        List<PendingDelete> pendingDeleteList = new ArrayList<>();
        pendingDeleteList.add(pendingDelete);
        pendingDeleteList.add(pendingDelete2);

        when(collectionWithDeletes.reviewedUris()).thenReturn(urisToUpdate);
        when(collectionWithDeletes.getDescription()).thenReturn(mockCollectionDescriptionWithDeletes);
        when(mockCollectionDescriptionWithDeletes.getPendingDeletes()).thenReturn(pendingDeleteList);
    }

    @After
    public void tearDown() throws Exception {
        mockitoAnnotations.close();
        mockConfiguration.close();
    }

    @Test
    public void payloadCacheAPIIsCorrect() {
        when(Configuration.isLegacyCacheAPIEnabled()).thenReturn(true);

        PublishNotification publishNotification = new PublishNotification(collection, null, null);

        String expectedPayload = "{" +
                "\"collection_id\":\"cake-1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef\"," +
                "\"release_time\":\"2021-01-05T17:00:00.000Z\"," +
                "\"path\":\"/economy/inflationandprices/articles/producerpriceinflation/latest\"" +
                "}";

        boolean hasExpectedPayload = publishNotification.getLegacyCacheApiPayloads()
                .stream()
                .map(Serialiser::serialise)
                .anyMatch(jsonPayload -> jsonPayload.equals(expectedPayload));

        assertTrue(hasExpectedPayload);
    }

    @Test
    public void payloadCacheAPIIsCorrectWithDeletes() {
        when(Configuration.isLegacyCacheAPIEnabled()).thenReturn(true);

        PublishNotification publishNotification = new PublishNotification(collectionWithDeletes);
        assertEquals(5, publishNotification.getLegacyCacheApiPayloads().size());

        String expectedPayload = "{" +
                "\"collection_id\":\"cake-1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef\"," +
                "\"release_time\":\"2021-01-05T17:00:00.000Z\"," +
                "\"path\":\"/economy/inflationandprices/articles/producerpriceinflation/latest\"" +
                "}";

        String expectedPayloadDelete = "{" +
                "\"collection_id\":\"cake-1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef\"," +
                "\"path\":\"/economy/inflationandprices/bulletins/producerpriceinflation/october2022\"" +
                "}";

        List<String> jsonPayloads = publishNotification.getLegacyCacheApiPayloads()
                .stream()
                .map(Serialiser::serialise)
                .collect(Collectors.toList());

        boolean hasExpectedPayload = jsonPayloads.contains(expectedPayload);
        boolean hasExpectedPayloadDelete = jsonPayloads.contains(expectedPayloadDelete);

        assertTrue(hasExpectedPayload);
        assertTrue(hasExpectedPayloadDelete);
    }

    @Test
    public void canCreatePublishNotificationWithURLsSetToNullWhenCacheEnabled() {
        when(Configuration.isLegacyCacheAPIEnabled()).thenReturn(true);

        PublishNotification publishNotification = new PublishNotification(collection, null, null);
        assertNotNull(publishNotification);
    }

    @Test
    public void canCreatePublishNotificationWithURLsSetToNullWhenCacheDisabled() {
        when(Configuration.isLegacyCacheAPIEnabled()).thenReturn(false);

        PublishNotification publishNotification = new PublishNotification(collection, null, null);
        assertNotNull(publishNotification);
    }


    @Test
    public void publishNotificationWhenCacheAPIEnabledReturns5PayloadsTest() throws IOException {
        when(Configuration.isLegacyCacheAPIEnabled()).thenReturn(true);
        urisToUpdate.add("/economy/inflationandpriceindices/bulletins/producerpriceinflation/october2022/30d7d6c2/");
        when(collection.reviewedUris()).thenReturn(urisToUpdate);

        assertEquals(4, collection.reviewedUris().size());

        PublishNotification publishNotification = new PublishNotification(collection);
        assertEquals(5, publishNotification.getLegacyCacheApiPayloads().size());

        List<String> expectedURIs = new ArrayList<>();
        expectedURIs.add("/economy/inflationandprices/articles/producerpriceinflation/latest");
        expectedURIs.add("/economy/inflationandprices/articles/producerpriceinflation/october2022");
        expectedURIs.add("/economy/inflationandprices/articles/producerpriceinflation/october2023");
        expectedURIs.add("/economy/inflationandpriceindices/bulletins/producerpriceinflation/october2022");
        expectedURIs.add("/economy/inflationandpriceindices/bulletins/producerpriceinflation/latest");

        boolean hasExpectedURIs = publishNotification.getLegacyCacheApiPayloads()
                .stream()
                .map(payload -> payload.uriToUpdate)
                .allMatch(expectedURIs::contains);

        assertTrue(hasExpectedURIs);
    }

    @Test
    public void publishNotificationWhenCacheAPIEnabledTest() throws IOException {
        when(Configuration.isLegacyCacheAPIEnabled()).thenReturn(true);

        assertEquals(3, collection.reviewedUris().size());

        PublishNotification publishNotification = new PublishNotification(collection);
        assertEquals(3, publishNotification.getLegacyCacheApiPayloads().size());

        Http mockHttp = mock(Http.class);
        ProtocolVersion version = new ProtocolVersion("1", 1, 0);
        BasicStatusLine basicStatusLine = new BasicStatusLine(version, 200, "OK");

        WebsiteResponse websiteResponse = new WebsiteResponse();
        websiteResponse.setMessage("OK");
        Response<Object> response = new Response<>(basicStatusLine, websiteResponse);
        when(mockHttp.put(any(), any(), any(), any())).thenReturn(response);

        LegacyCacheApiClient.sendPayloads(mockHttp, legacyCacheApiHost, publishNotification.getLegacyCacheApiPayloads());

        verify(mockHttp, times(3)).put(endpointCapture.capture(), payloadCapture.capture(), classCapture.capture(), any());

        List<String> expectedEndpoints = new ArrayList<>();
        expectedEndpoints.add("/v1/cache-times/a4706c4475f284457782beba7225283c");
        expectedEndpoints.add("/v1/cache-times/e1b8d3843ea2dc98211bde230a1bc1b6");
        expectedEndpoints.add("/v1/cache-times/16dc6c498a4e4b20dbcb66b00971ab3a");

        boolean expectedEndpointsHaveBeenCalled = endpointCapture.getAllValues()
                .stream()
                .map(endpoint -> endpoint.url().toString())
                .allMatch(url -> {
                    for (String expectedEndpoint : expectedEndpoints) {
                        if (url.contains(expectedEndpoint)) {
                            return true;
                        }
                    }
                    return false;
                });

        assertTrue(expectedEndpointsHaveBeenCalled);

        List<String> expectedPaths = new ArrayList<>();
        expectedPaths.add("/economy/inflationandprices/articles/producerpriceinflation/latest");
        expectedPaths.add("/economy/inflationandprices/articles/producerpriceinflation/october2022");
        expectedPaths.add("/economy/inflationandprices/articles/producerpriceinflation/october2023");

        boolean expectedPathsHaveBeenSet = payloadCapture.getAllValues()
                .stream()
                .map(payload -> payload.uriToUpdate)
                .allMatch(expectedPaths::contains);

        assertTrue(expectedPathsHaveBeenSet);
    }

    @Test
    public void publishNotificationWhenCacheAPIEnabledAndUNLOCKTest() throws IOException {
        when(Configuration.isLegacyCacheAPIEnabled()).thenReturn(true);

        assertEquals(3, collectionWithDeletes.reviewedUris().size());
        assertEquals(2, collectionWithDeletes.getDescription().getPendingDeletes().size());

        PublishNotification publishNotification = new PublishNotification(collectionWithDeletes);
        assertEquals(5, publishNotification.getLegacyCacheApiPayloads().size());

        Http mockHttp = mock(Http.class);
        ProtocolVersion version = new ProtocolVersion("1", 1, 0);
        BasicStatusLine basicStatusLine = new BasicStatusLine(version, 200, "OK");

        WebsiteResponse websiteResponse = new WebsiteResponse();
        websiteResponse.setMessage("OK");
        Response<Object> response = new Response<>(basicStatusLine, websiteResponse);
        when(mockHttp.put(any(), any(), any(), any())).thenReturn(response);

        publishNotification.removePublishDateForUnlockedEvents(EventType.UNLOCKED);
        LegacyCacheApiClient.sendPayloads(mockHttp, legacyCacheApiHost, publishNotification.getLegacyCacheApiPayloads());

        verify(mockHttp, times(5)).put(endpointCapture.capture(), payloadCapture.capture(), classCapture.capture(), any());

        List<String> expectedPaths = new ArrayList<>();
        expectedPaths.add("/economy/inflationandprices/articles/producerpriceinflation/latest");
        expectedPaths.add("/economy/inflationandprices/articles/producerpriceinflation/october2022");
        expectedPaths.add("/economy/inflationandprices/articles/producerpriceinflation/october2023");
        expectedPaths.add("/economy/inflationandprices/bulletins/producerpriceinflation/october2022");
        expectedPaths.add("/economy/inflationandprices/bulletins/producerpriceinflation/october2023");

        boolean hasExpectedPaths = payloadCapture.getAllValues().stream().map(payload -> payload.uriToUpdate).allMatch(expectedPaths::contains);

        assertTrue(hasExpectedPaths);

        boolean allDatesAreNull = payloadCapture.getAllValues().stream().map(payload -> payload.publishDate).allMatch(Objects::isNull);

        assertTrue(allDatesAreNull);
    }

    @Test
    public void publishNotificationWhenCacheAPIEnabledAndCollectionHasBothUpdatesAndDeletesTest() throws IOException {
        when(Configuration.isLegacyCacheAPIEnabled()).thenReturn(true);

        assertEquals(3, collectionWithDeletes.reviewedUris().size());
        assertEquals(2, collectionWithDeletes.getDescription().getPendingDeletes().size());

        PublishNotification publishNotification = new PublishNotification(collectionWithDeletes);
        assertEquals(5, publishNotification.getLegacyCacheApiPayloads().size());

        Http mockHttp = mock(Http.class);
        ProtocolVersion version = new ProtocolVersion("1", 1, 0);
        BasicStatusLine basicStatusLine = new BasicStatusLine(version, 200, "OK");

        WebsiteResponse websiteResponse = new WebsiteResponse();
        websiteResponse.setMessage("OK");
        Response<Object> response = new Response<>(basicStatusLine, websiteResponse);
        when(mockHttp.put(any(), any(), any(), any())).thenReturn(response);

        LegacyCacheApiClient.sendPayloads(mockHttp, legacyCacheApiHost, publishNotification.getLegacyCacheApiPayloads());

        verify(mockHttp, times(5)).put(endpointCapture.capture(), payloadCapture.capture(), classCapture.capture(), any());

        List<String> expectedPaths = new ArrayList<>();
        expectedPaths.add("/economy/inflationandprices/articles/producerpriceinflation/latest");
        expectedPaths.add("/economy/inflationandprices/articles/producerpriceinflation/october2022");
        expectedPaths.add("/economy/inflationandprices/articles/producerpriceinflation/october2023");
        expectedPaths.add("/economy/inflationandprices/bulletins/producerpriceinflation/october2022");
        expectedPaths.add("/economy/inflationandprices/bulletins/producerpriceinflation/october2023");

        boolean hasExpectedPaths = payloadCapture.getAllValues().stream().map(payload -> payload.uriToUpdate).allMatch(expectedPaths::contains);

        assertTrue(hasExpectedPaths);

        Long expectedNumberOfPayloadsWithoutPublishDate = 2L;
        Long payloadsWithoutPublishDate = payloadCapture.getAllValues().stream().filter(payload -> payload.publishDate == null).count();

        assertEquals(expectedNumberOfPayloadsWithoutPublishDate, payloadsWithoutPublishDate);
    }

    @Test
    public void publishNotificationNoClientCallsWhenNoUrlsWhenCacheAPIEnabledTest() throws IOException {
        when(Configuration.isLegacyCacheAPIEnabled()).thenReturn(true);

        urisToUpdate = new ArrayList<>();
        when(collection.reviewedUris()).thenReturn(urisToUpdate);

        PublishNotification publishNotification = new PublishNotification(collection);
        assertEquals(0, publishNotification.getLegacyCacheApiPayloads().size());

        Http httpMock = mock(Http.class);

        LegacyCacheApiClient.sendPayloads(httpMock, legacyCacheApiHost, new ArrayList<>());

        verify(httpMock, never()).postJson(any(), any(), any());
    }

    @Test
    public void publishNotificationWhenCacheAPIEnabledDoesNotSendRequestToAPIWhenEventTypeIsPublishedTest() {
        when(Configuration.isLegacyCacheAPIEnabled()).thenReturn(true);

        PublishNotification publishNotification = new PublishNotification(collection);

        try (MockedStatic<LegacyCacheApiClient> ignored = mockStatic(LegacyCacheApiClient.class)) {
            publishNotification.sendNotification(EventType.PUBLISHED);

            verifyNoInteractions(LegacyCacheApiClient.class);
        }
    }

    @Test
    public void publishNotificationWhenCacheAPIEnabledSendsRequestToAPIWhenEventTypeIsApprovedTest() {
        when(Configuration.isLegacyCacheAPIEnabled()).thenReturn(true);

        PublishNotification publishNotification = new PublishNotification(collection);

        try (MockedStatic<LegacyCacheApiClient> mockController = mockStatic(LegacyCacheApiClient.class)) {
            publishNotification.sendNotification(EventType.APPROVED);

            mockController.verify(() -> LegacyCacheApiClient.sendPayloads(any(), any(), any()));
        }
    }

    @Test
    public void publishNotificationWhenCacheAPIEnabledSendsRequestToAPIWhenEventTypeIsUnlockedTest() {
        when(Configuration.isLegacyCacheAPIEnabled()).thenReturn(true);

        PublishNotification publishNotification = new PublishNotification(collection);

        try (MockedStatic<LegacyCacheApiClient> mockController = mockStatic(LegacyCacheApiClient.class)) {
            publishNotification.sendNotification(EventType.UNLOCKED);

            mockController.verify(() -> LegacyCacheApiClient.sendPayloads(any(), any(), any()));
        }
    }

    @Test
    public void constructorDoesNotThrowWhenUrlsIsNullTest() throws IOException {
        urisToUpdate = new ArrayList<>();
        urisToUpdate.add(null);
        when(collection.reviewedUris()).thenReturn(urisToUpdate);

        when(Configuration.isLegacyCacheAPIEnabled()).thenReturn(true);

        List<Host> hosts = new ArrayList<>();
        hosts.add(new Host("http://localhost:29100"));
        when(Configuration.getWebsiteHosts()).thenReturn(hosts);

        assertTrue(Configuration.isLegacyCacheAPIEnabled());

        PublishNotification publishNotification = new PublishNotification(collection, null, null);

        assertNotNull(publishNotification);
    }

    @Test
    public void checkZebedeeCodeChecksumIsSameAsProxyCodeChecksum() throws IOException {
        when(Configuration.isLegacyCacheAPIEnabled()).thenReturn(true);

        String uri = "/file?uri=/economy/inflationandpriceindices/adhocs/009581cpiinflationbetween2010and2018/cpiinflationbetween2010and2018.xls";
        urisToUpdate = new ArrayList<>();
        urisToUpdate.add(uri);

        when(collection.reviewedUris()).thenReturn(urisToUpdate);

        PublishNotification publishNotification = new PublishNotification(collection);

        String resultUri = publishNotification.getLegacyCacheApiPayloads().iterator().next().uriToUpdate;
        String uriChecksum = EncryptionUtils.createMD5Checksum(resultUri);

        String expectedChecksum = "4c0185c09adc47b557ff3c6db81be8cc"; // checksum from proxy generated should match for given url

        assertEquals(expectedChecksum, uriChecksum);
    }
}
