package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dp.image.api.client.ImageClient;
import com.github.onsdigital.dp.image.api.client.exception.ImageAPIException;
import com.github.onsdigital.dp.image.api.client.model.Image;
import com.github.onsdigital.dp.image.api.client.model.Images;
import com.github.onsdigital.zebedee.model.Collection;

import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

/**
 * Image related services
 */
public class ImageServiceImpl implements ImageService {

    public static final String STATE_CREATED = "created";
    public static final String STATE_DELETED = "deleted";
    private ImageClient imageClient;

    /**
     * Construct a new instance of the the image service
     *
     * @param imageClient An instance of an Image API client to be used by the service
     */
    public ImageServiceImpl(ImageClient imageClient) {
        this.imageClient = imageClient;
    }


    /**
     * Publish the images contained in the given collection.
     */
    @Override
    public void publishImagesInCollection(Collection collection) throws IOException, ImageAPIException {

        Images images = imageClient.getImages(collection.getId());
        info().data("publishing", true).data("collectionId", collection.getId())
                .data("numberOfImages", images.getTotalCount())
                .log("publishing images in collection");

        for (Image image : images.getItems()) {
            switch (image.getState()) {
                case STATE_CREATED:
                case STATE_DELETED:
                    info().data("publishing", true).data("collectionId", collection.getId())
                            .data("imageId", image.getId())
                            .data("imageState",image.getState())
                            .log("skipping publish of image");
                    break;
                default:
                    info().data("publishing", true).data("collectionId", collection.getId())
                            .data("imageId", image.getId())
                            .log("publishing image");
                    imageClient.publishImage(image.getId());
            }
        }

        // Image API does not implement paging. Capture scenario if it is implemented unexpectedly.
        if (images.getTotalCount() > images.getCount()) {
            throw new IOException("Not all images have been published due to API paging");
        }
    }

}
