package com.github.onsdigital.zebedee.model.publishing.legacycacheapi;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class LegacyCacheApiPayloadBuilderTest {
    @RunWith(MockitoJUnitRunner.class)
    public static class LegacyCacheApiPayloadNonParameterisedTest {
        @Mock
        private Collection collection;
        private List<String> urisToUpdate;
        private AutoCloseable mockitoAnnotations;

        private String testCollectionID = "cake-1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
        private String testCollectionName = "The Cake Collection";

        @Before
        public void setUp() throws IOException {
            mockitoAnnotations = MockitoAnnotations.openMocks(this);
            urisToUpdate = new ArrayList<>();

            Date publishDate = new Date(1609866000000L);

            CollectionDescription mockCollectionDescription = mock(CollectionDescription.class);
            when(mockCollectionDescription.getId()).thenReturn(testCollectionID);
            when(mockCollectionDescription.getName()).thenReturn(testCollectionName);

            when(mockCollectionDescription.getPublishDate()).thenReturn(publishDate);

            when(collection.getDescription()).thenReturn(mockCollectionDescription);
            when(collection.reviewedUris()).thenReturn(urisToUpdate);
        }

        @After
        public void tearDown() throws Exception {
            mockitoAnnotations.close();
            urisToUpdate.clear();
        }

        @Test
        public void NotificationPayloadCacheApiForBulletinLatestReturnsTwoTest() {
            String testUriBulletin = "/economy/inflationandpriceindices/bulletins/producerpriceinflation/october2022/30d7d6c2/";
            urisToUpdate.add(testUriBulletin);

            java.util.Collection<LegacyCacheApiPayload> payloads = new LegacyCacheApiPayloadBuilder.Builder().collection(collection).build().getPayloads();
            assertEquals(2, payloads.size());

            String expectedURI = "/economy/inflationandpriceindices/bulletins/producerpriceinflation/latest";
            boolean hasExpectedURI = payloads.stream().map(payload -> payload.uriToUpdate).anyMatch(uri -> uri.equals(expectedURI));

            assertTrue(hasExpectedURI);
        }
       
        @Test
        public void NotificationPayloadCacheApiForArticleLatestReturnsTwoTest() {
            String testUriArticle = "/economy/inflationandpriceindices/articles/consumerpricesdevelopmentplan/updatedaugust2024";
            urisToUpdate.add(testUriArticle);

            java.util.Collection<LegacyCacheApiPayload> payloads = new LegacyCacheApiPayloadBuilder.Builder().collection(collection).build().getPayloads();
            assertEquals(2, payloads.size());

            String expectedURI = "/economy/inflationandpriceindices/articles/consumerpricesdevelopmentplan/latest";
            boolean hasExpectedURI = payloads.stream().map(payload -> payload.uriToUpdate).anyMatch(uri -> uri.equals(expectedURI));

            assertTrue(hasExpectedURI);
        }
       
        @Test
        public void NotificationPayloadCacheApiForCompendiaLatestReturnsTwoTest() {
            String testUriCompendia = "/economy/grossdomesticproductgdp/compendium/unitedkingdomnationalaccountsthebluebook/2024/";
            urisToUpdate.add(testUriCompendia);

            java.util.Collection<LegacyCacheApiPayload> payloads = new LegacyCacheApiPayloadBuilder.Builder().collection(collection).build().getPayloads();
            assertEquals(2, payloads.size());

            String expectedURI = "/economy/grossdomesticproductgdp/compendium/unitedkingdomnationalaccountsthebluebook/latest";
            boolean hasExpectedURI = payloads.stream().map(payload -> payload.uriToUpdate).anyMatch(uri -> uri.equals(expectedURI));

            assertTrue(hasExpectedURI);
        }

        @Test
        public void NotificationPayloadCacheApiForBulletinLatestReturnsTwoFromParamUrlTest() {
            String testUriWithBulletinAsQueryParam = "/generator?uri=/economy/inflationandpriceindices/bulletins/producerpriceinflation/october2022/30d7d6c2/&format=csv";

            urisToUpdate.clear();
            urisToUpdate.add(testUriWithBulletinAsQueryParam);
            java.util.Collection<LegacyCacheApiPayload> payloads = new LegacyCacheApiPayloadBuilder.Builder().collection(collection).build().getPayloads();
            assertEquals(2, payloads.size());

            String expectedURI = "/economy/inflationandpriceindices/bulletins/producerpriceinflation/latest";
            boolean hasExpectedURI = payloads.stream().map(payload -> payload.uriToUpdate).anyMatch(uri -> uri.equals(expectedURI));

            assertTrue(hasExpectedURI);
        }

        @Test
        public void NotificationPayloadCacheApiForBulletinLatestReturnsOneTest() {
            String testUriBulletinWithLatestString = "/economy/inflationandpriceindices/bulletins/producerpriceinflation/latest/30d7d6c2/";

            urisToUpdate.clear();
            urisToUpdate.add(testUriBulletinWithLatestString);
            java.util.Collection<LegacyCacheApiPayload> payloads = new LegacyCacheApiPayloadBuilder.Builder().collection(collection).build().getPayloads();
            assertEquals(1, payloads.size());
            assertTrue(payloads.iterator().next().uriToUpdate.contains("/economy/inflationandpriceindices/bulletins/producerpriceinflation/latest"));
        }
       
        @Test
        public void NotificationPayloadCacheApiForArticleLatestReturnsOneTest() {
            String testUriArticleWithLatestString = "/economy/inflationandpriceindices/articles/consumerpricesdevelopmentplan/latest/updatedaugust2024/";

            urisToUpdate.clear();
            urisToUpdate.add(testUriArticleWithLatestString);
            java.util.Collection<LegacyCacheApiPayload> payloads = new LegacyCacheApiPayloadBuilder.Builder().collection(collection).build().getPayloads();
            assertEquals(1, payloads.size());
            assertTrue(payloads.iterator().next().uriToUpdate.contains("/economy/inflationandpriceindices/articles/consumerpricesdevelopmentplan/latest"));
        }
        
        @Test
        public void NotificationPayloadCacheApiForCompendiaLatestReturnsOneTest() {
            String testUriCompendiaWithLatestString = "/economy/grossdomesticproductgdp/compendium/unitedkingdomnationalaccountsthebluebook/latest/2025/";

            urisToUpdate.clear();
            urisToUpdate.add(testUriCompendiaWithLatestString);
            java.util.Collection<LegacyCacheApiPayload> payloads = new LegacyCacheApiPayloadBuilder.Builder().collection(collection).build().getPayloads();
            assertEquals(1, payloads.size());
            assertTrue(payloads.iterator().next().uriToUpdate.contains("/economy/grossdomesticproductgdp/compendium/unitedkingdomnationalaccountsthebluebook/latest"));
        }

        @Test
        public void NotificationPayloadCacheApiForFileReturnsFileURITest() {
            String testUriFile = "/economy/grossdomesticproductgdp/compendium/unitedkingdomnationalaccountsthebluebook/latest/2025/test.csv";

            urisToUpdate.clear();
            urisToUpdate.add(testUriFile);
            java.util.Collection<LegacyCacheApiPayload> payloads = new LegacyCacheApiPayloadBuilder.Builder().collection(collection).build().getPayloads();
            assertEquals(2, payloads.size());

            String expectedFileURI = "/file?uri=/economy/grossdomesticproductgdp/compendium/unitedkingdomnationalaccountsthebluebook/latest/2025/test.csv";
            boolean hasExpectedFileURI = payloads.stream().map(payload -> payload.uriToUpdate).anyMatch(uri -> uri.equals(expectedFileURI));

            String expectedURI = "/economy/grossdomesticproductgdp/compendium/unitedkingdomnationalaccountsthebluebook/latest";
            boolean hasExpectedURI = payloads.stream().map(payload -> payload.uriToUpdate).anyMatch(uri -> uri.equals(expectedURI));

            assertTrue(hasExpectedFileURI);
            assertTrue(hasExpectedURI);
        }

        @Test
        public void NotificationPayloadCacheApiForFileReturnsCollectionDetailsTest() {
            String testUriFile = "/economy/grossdomesticproductgdp/compendium/unitedkingdomnationalaccountsthebluebook/latest/2025/test.csv";

            urisToUpdate.clear();
            urisToUpdate.add(testUriFile);
            java.util.Collection<LegacyCacheApiPayload> payloads = new LegacyCacheApiPayloadBuilder.Builder().collection(collection).build().getPayloads();
            assertEquals(2, payloads.size());

            boolean hasExpectedCollectionId = payloads.stream().map(payload -> payload.collectionId).anyMatch(id -> id.equals(testCollectionID));
            boolean hasExpectedCollectionName = payloads.stream().map(payload -> payload.collectionTitle).anyMatch(name -> name.equals(testCollectionName));

            assertTrue(hasExpectedCollectionId);
            assertTrue(hasExpectedCollectionName);
        }
    }
}
