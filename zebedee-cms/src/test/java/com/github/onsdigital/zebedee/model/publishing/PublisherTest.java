package com.github.onsdigital.zebedee.model.publishing;

import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.service.StaticFilesService;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PublisherTest {

    private Publisher publisher;

    @Before
    public void setup() throws IOException {
        this.publisher = new Publisher();
    }


    @Test
    public void testConvertUriWithJsonForEvent() {

        //Given {a single uri with data.json ready to be sent to kafka}
        String testUri = "/testUri0/data.json";

        //When {sending a uris to kafka}
        String actual = publisher.convertUriForEvent(testUri);

        System.out.println(actual);

        //Then {uri does not have "data.json" string}
        assertFalse(actual.contains("/testUri0/data.json"));
        assertTrue(actual.contains("/testUri0"));
    }

    @Test
    public void testConvertUriWithOutJsonForEvent() {

        //Given {a single uri without data.json ready to be sent to kafka}
        String testUri1 = "/testUri1";

        //When {sending a uri to kafka}
        String actual = publisher.convertUriForEvent(testUri1);

        //Then {uris returns the original string}
        assertTrue(actual.contains("/testUri1"));
    }

    @Test
    public void testisValidCMDDatasetURISuccess() {

        //Given {A valid uri is passed}
        String testUri = "/datasets/cpih01/editions/timeseries/versions/version";

        //When {Check for uri validity}
        boolean actual = publisher.isValidCMDDatasetURI(testUri);

        //Then {The uri is valid}
        assertTrue(actual);
    }

    @Test
    public void testisValidCMDDatasetURIFailure() {

        //Given {An invalid uri is passed}
        String testUri = "/dataset/cpih/editions/timeseries/";

        //When {Check for uri validity}
        boolean actual = publisher.isValidCMDDatasetURI(testUri);

        //Then {The uri is not valid}
        assertFalse(actual);
    }

    @Test
    public void testisValidCMDDatasetURISuccessWithHyphen() {

        //Given {A valid uri with hypen is passed}
        String testUri = "/datasets/cpih01-test-7/editions/time-series/versions/8";

        //When {Check for uri validity}
        boolean actual = publisher.isValidCMDDatasetURI(testUri);

        //Then {The uri is valid}
        assertTrue(actual);
    }

    String emailAddress = "bogusEmail@ons.gov.uk";
    @Mock
    Collection mockCollection;
    @Mock
    CollectionDescription mockCollectionDescription;
    @Mock
    Content mockContent;
    @Mock
    StaticFilesService mockFileService;

    @Before
    public void beforeEach() throws Exception{
        //Given {A Collection with no files}
        when(mockContent.uris()).thenReturn(new ArrayList<String>(0));

        when(mockCollectionDescription.getId()).thenReturn(randomCollectionId());
        when(mockCollectionDescription.getPublishTransactionIds()).thenReturn(new TreeMap<String,String>());

        when(mockCollection.getReviewed()).thenReturn(mockContent);
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        // And {features are disabled except static files}
        System.setProperty(CMSFeatureFlags.ENABLE_DATASET_IMPORT, "false");
        System.setProperty(CMSFeatureFlags.ENABLE_IMAGE_PUBLISHING, "false");
        System.setProperty(CMSFeatureFlags.ENABLE_VERIFY_PUBLISH_CONTENT, "false");
        System.setProperty(CMSFeatureFlags.ENABLE_KAFKA, "false");
        System.setProperty(CMSFeatureFlags.ENABLE_STATIC_FILES_PUBLISHING, "false");
        System.setProperty(CMSFeatureFlags.ENABLE_INTERACTIVES_PUBLISHING, "false");

        Publisher.staticFilesServiceSupplier = () ->  mockFileService;

    }


    @Test
    public void whenFilePublishingEnableStaticFilesAreSuccessfullyPublished() throws Exception{
        // Given {A Collection with no files}
        // Default

        // And {features are disabled except static files}
        System.setProperty(CMSFeatureFlags.ENABLE_STATIC_FILES_PUBLISHING, "true");
        CMSFeatureFlags.reset();
        //When {executePublish is called}
        boolean executeResult = Publisher.executePublish(mockCollection, null, emailAddress);

        //Then {return is true (success)}
        assertTrue(executeResult);

        // ensure that static file published was called
        verify(mockFileService, times(1)).publishCollection(any(Collection.class));
    }

    @Test
    public void whenFilePublishingEnableStaticFilesPublishingThrowsException() throws Exception{
        // Given {A Collection with no files}
        // Default

        // And {features are disabled except static files}
        System.setProperty(CMSFeatureFlags.ENABLE_STATIC_FILES_PUBLISHING, "true");
        CMSFeatureFlags.reset();

        doThrow(new RuntimeException("BROKEN"))
                .when(mockFileService)
                .publishCollection(any());


        //When {executePublish is called}
        boolean executeResult = Publisher.executePublish(mockCollection, null, emailAddress);

        //Then {return is true (success)}
        assertFalse(executeResult);

        // ensure that static file published was called
        verify(mockFileService, times(1)).publishCollection(any(Collection.class));
    }

    @Test
    public void whenFilePublishingEnableStaticFilesArePublishedToo() throws Exception{
        //Given {A Collection with no files}
        //default
        System.setProperty(CMSFeatureFlags.ENABLE_STATIC_FILES_PUBLISHING, "true");
        CMSFeatureFlags.reset();

        // And {features are disabled except static files}


        //When {executePublish is called}
        boolean executeResult = Publisher.executePublish(mockCollection, null, emailAddress);

        //Then {return is true (success)}
        assertTrue(executeResult);

        // ensure that static file published was called
        verify(mockFileService, times(1)).publishCollection(any(Collection.class));

    }

    @Test
    public void testIfEmptyCollectionWorks() throws Exception{
        //Given {A Collection with no files}
        // default


        // And {features are disabled except static files}
        // default - all features disabled
        CMSFeatureFlags.reset();

        //When {executePublish is called}
        boolean executeResult = Publisher.executePublish(mockCollection, null, emailAddress);

        //Then {return is true (success)}
        assertTrue(executeResult);

        // ensure that static file published was not called
        verify(mockFileService, never()).publishCollection(any(Collection.class));
    }

    private String randomCollectionId() {
        return RandomStringUtils.random(20, true, true);
    }


}