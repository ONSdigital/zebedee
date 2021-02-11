package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dp.image.api.client.ImageClient;
import com.github.onsdigital.dp.image.api.client.exception.ImageAPIException;
import com.github.onsdigital.dp.image.api.client.model.Image;
import com.github.onsdigital.dp.image.api.client.model.Images;
import com.github.onsdigital.zebedee.model.Collection;

import java.io.IOException;

/**
 * Image related services
 */
public class ZebedeeImageService implements ImageService {

    private ImageClient imageClient;

    public ZebedeeImageService(ImageClient imageClient) {
        this.imageClient = imageClient;
    }


    /**
     * Publish the images contained in the given collection.
     */
    @Override
    public void publishImagesInCollection(Collection collection) throws IOException, ImageAPIException {

        Images images = imageClient.getImagesWithCollectionId(collection.getId());

        for (Image image : images.getItems()) {
            imageClient.publishImage(image.getId());
        }

    }

}
