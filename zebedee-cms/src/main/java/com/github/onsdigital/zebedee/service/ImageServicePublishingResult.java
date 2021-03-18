package com.github.onsdigital.zebedee.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to contain details of the result of publishing images
 */
public class ImageServicePublishingResult {
    private int totalImages;
    private List<ImageDetails> unpublishedImages = new ArrayList<>();

    /**
     * Construct a new instance for the given number of images
     *
     * @param totalImages Total number of images in the collection
     */
    public ImageServicePublishingResult(int totalImages) {
        this.totalImages = totalImages;
    }

    public int getTotalImages() {
        return totalImages;
    }

    public List<ImageDetails> getUnpublishedImages() {
        return unpublishedImages;
    }

    /**
     * Add details of an unpublished image to the results
     *
     * @param id     ID of the unpublished image
     * @param status Status of the unpublished image
     */
    public void addUnpublishedImage(String id, String status) {
        unpublishedImages.add(new ImageDetails(id, status));
    }

    /**
     * Details of a specific image
     */
    public static class ImageDetails {
        private String id;
        private String status;

        /**
         * Constuct an instancce of ImageDetails with a given ID and Status
         *
         * @param id     ID of the image
         * @param status Status of the  image
         */
        public ImageDetails(String id, String status) {
            this.id = id;
            this.status = status;
        }

        public String getId() {
            return id;
        }

        public String getStatus() {
            return status;
        }

    }
}
