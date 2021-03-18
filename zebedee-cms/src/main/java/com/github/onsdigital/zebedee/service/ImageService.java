package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dp.image.api.client.exception.ImageAPIException;
import com.github.onsdigital.zebedee.model.Collection;

import java.io.IOException;

/**
 * Provides high level images functionality
 */
public interface ImageService {

    /**
     * Publish the images contained in the given collection.
     */
    ImageServicePublishingResult publishImagesInCollection(Collection collection) throws IOException, ImageAPIException;

}
