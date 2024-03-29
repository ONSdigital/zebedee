package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dp.image.api.client.ImageClient;
import com.github.onsdigital.dp.image.api.client.exception.ImageAPIException;
import com.github.onsdigital.dp.image.api.client.model.Image;
import com.github.onsdigital.dp.image.api.client.model.Images;
import com.github.onsdigital.zebedee.model.Collection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ImageServiceImplTest {

    private static final String COLLECTION_ID = "col123";
    private static final String IMAGE1 = "i1";
    private static final String IMAGE2 = "i2";
    private static final String IMAGE3 = "i3";
    private static final String IMAGE4 = "i4";
    private static final String STATE_CREATED = "created";
    private static final String STATE_UPLOADED = "uploaded";
    private static final String STATE_IMPORTING = "importing";
    private static final String STATE_FAILED_IMPORT = "failed_import";
    private static final String STATE_IMPORTED = "imported";
    private static final String STATE_DELETED = "deleted";

    ImageAPIException apiException = new ImageAPIException("error", 123);

    Collection mockCollection = mock(Collection.class);
    ImageClient mockImageAPI = mock(ImageClient.class);

    @Before
    public void setup() throws Exception {
        when(mockCollection.getId()).thenReturn(COLLECTION_ID);
        doNothing().when(mockImageAPI).publishImage(anyString());
    }

    @Test
    public void testPublishImagesInCollection_threeImages() throws Exception {
        // Given an api that returns three images
        when(mockImageAPI.getImages(COLLECTION_ID)).thenReturn(createImportedTestImages(IMAGE1, IMAGE2, IMAGE3));
        ImageService imageService = new ImageServiceImpl(mockImageAPI);

        // When publish is called on the collection
        imageService.publishImagesInCollection(mockCollection);

        // Then publishImage should be called on the API for each image.
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockImageAPI, times(3)).publishImage(captor.capture());
        assertEquals(IMAGE1, captor.getAllValues().get(0));
        assertEquals(IMAGE2, captor.getAllValues().get(1));
        assertEquals(IMAGE3, captor.getAllValues().get(2));
    }

    @Test
    public void testPublishImagesInCollection_withCreatedORDeletedSkipped() throws Exception {
        // Given an api that returns created,imported and deleted images
        when(mockImageAPI.getImages(COLLECTION_ID)).thenReturn(createTestImages(Arrays.asList(
                createTestImage(IMAGE1,STATE_CREATED),
                createTestImage(IMAGE2,STATE_IMPORTED),
                createTestImage(IMAGE3,STATE_DELETED)
                )));
        ImageService imageService = new ImageServiceImpl(mockImageAPI);

        // When publish is called on the collection
        ImageServicePublishingResult result = imageService.publishImagesInCollection(mockCollection);
        assertNotNull(result);
        assertEquals(3,result.getTotalImages());

        // Then publishImage should not be called on the API for the created or deleted images.
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockImageAPI, times(1)).publishImage(captor.capture());
        assertEquals(IMAGE2, captor.getAllValues().get(0));
    }

    @Test
    public void testPublishImagesInCollection_withImportingORFailedImportUnpublished() throws Exception {
        // Given an api that returns importing,failed_import and imported images
        when(mockImageAPI.getImages(COLLECTION_ID)).thenReturn(createTestImages(Arrays.asList(
                createTestImage(IMAGE1,STATE_UPLOADED),
                createTestImage(IMAGE2,STATE_IMPORTING),
                createTestImage(IMAGE3,STATE_FAILED_IMPORT),
                createTestImage(IMAGE4,STATE_IMPORTED)
                )));
        ImageService imageService = new ImageServiceImpl(mockImageAPI);

        // When publish is called on the collection
        ImageServicePublishingResult result = imageService.publishImagesInCollection(mockCollection);
        assertNotNull(result);
        assertEquals(4,result.getTotalImages());
        assertNotNull(result.getUnpublishedImages());
        assertEquals(3,result.getUnpublishedImages().size());
        assertNotNull(result.getUnpublishedImages().get(0));
        assertEquals(IMAGE1, result.getUnpublishedImages().get(0).getId());
        assertNotNull(result.getUnpublishedImages().get(1));
        assertEquals(IMAGE2, result.getUnpublishedImages().get(1).getId());
        assertNotNull(result.getUnpublishedImages().get(2));
        assertEquals(IMAGE3, result.getUnpublishedImages().get(2).getId());

        // Then publishImage should not be called on the API for the created or deleted images.
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockImageAPI, times(1)).publishImage(captor.capture());
        assertEquals(IMAGE4, captor.getAllValues().get(0));
    }

    @Test
    public void testPublishImagesInCollection_zeroImages() throws Exception {
        // Given an api that returns zero images
        when(mockImageAPI.getImages(COLLECTION_ID)).thenReturn(createImportedTestImages());
        ImageService imageService = new ImageServiceImpl(mockImageAPI);

        // When publish is called on the collection
        imageService.publishImagesInCollection(mockCollection);

        // Then publishImage should not be called on the API.
        verify(mockImageAPI, times(0)).publishImage(anyString());
    }

    @Test(expected = ImageAPIException.class)
    public void testPublishImagesInCollection_getImagesException() throws Exception {
        // Given an api that returns throws an exception when getting publishable images
        when(mockImageAPI.getImages(COLLECTION_ID)).thenThrow(apiException);
        ImageService imageService = new ImageServiceImpl(mockImageAPI);

        // When publish is called on the collection
        imageService.publishImagesInCollection(mockCollection);

        // Then an ImageAPIException is expected
    }

    @Test(expected = ImageAPIException.class)
    public void testPublishImagesInCollection_publishImageException() throws Exception {
        // Given an api that returns a good image but throws an exception when publishing
        when(mockImageAPI.getImages(COLLECTION_ID)).thenReturn(createImportedTestImages(IMAGE1));
        doThrow(apiException).when(mockImageAPI).publishImage(anyString());
        ImageService imageService = new ImageServiceImpl(mockImageAPI);

        // When publish is called on the collection
        imageService.publishImagesInCollection(mockCollection);

        // Then an ImageAPIException is expected
    }

    @Test
    public void testPublishImagesInCollection_unexpectedPaging() throws Exception {
        // Given an api that returns one page of good images but with more pages available
        Images testImages = createImportedTestImages(IMAGE1, IMAGE2);
        testImages.setTotalCount(3);
        when(mockImageAPI.getImages(COLLECTION_ID)).thenReturn(testImages);
        ImageService imageService = new ImageServiceImpl(mockImageAPI);

        Exception ex = assertThrows(IOException.class, () -> imageService.publishImagesInCollection(mockCollection));

        // Then publishImage should be called on the API for each image that it does have.
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockImageAPI, times(2)).publishImage(captor.capture());
        assertEquals(IMAGE1, captor.getAllValues().get(0));
        assertEquals(IMAGE2, captor.getAllValues().get(1));

        // Then an IOException is expected (because zebedee uses IOExceptions inappropriately)
        assertEquals("Not all images have been published due to API paging", ex.getMessage());
    }

    private Image createTestImage(String id,String state) {
        Image image = new Image();
        image.setId(id);
        image.setState(state);
        return image;
    }

    private Images createTestImages(List<Image> imageItems) {
        Images images = new Images();
        images.setCount(imageItems.size());
        images.setTotalCount(imageItems.size());
        images.setItems(imageItems);
        return images;
    }

    private Images createImportedTestImages(String... ids) {
        List<Image> imageItems = new ArrayList<Image>();
        for (String id : ids) {
            imageItems.add(createTestImage(id, STATE_IMPORTED));
        }
        return createTestImages(imageItems);
    }
}


