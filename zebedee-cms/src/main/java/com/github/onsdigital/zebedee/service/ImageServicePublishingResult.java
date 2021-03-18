package com.github.onsdigital.zebedee.service;

import java.util.ArrayList;
import java.util.List;

public class ImageServicePublishingResult {
    private int totalImages;
    private List<ImageDetails> unpublishedImages = new ArrayList<>();

    public ImageServicePublishingResult(int totalImages) {
        this.totalImages = totalImages;
    }

    public int getTotalImages() {
        return totalImages;
    }

    public List<ImageDetails> getUnpublishedImages() {
        return unpublishedImages;
    }

    public void addUnpublishedImage(String id, String status) {
        unpublishedImages.add(new ImageDetails(id, status));
    }

    public static class ImageDetails {
        private String id;
        private String status;

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
