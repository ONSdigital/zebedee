package com.github.onsdigital.zebedee.service.publishedcollections;

public class PublishedCollection {
    private String id;
    private String name;
    private String publishDate;
    private String publishStartDate;
    private String publishEndDate;
    private PublishedItem[] publishResults;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public String getPublishEndDate() {
        return publishEndDate;
    }

    public void setPublishEndDate(String publishEndDate) {
        this.publishEndDate = publishEndDate;
    }

    public PublishedItem[] getPublishResults() {
        return publishResults;
    }

    public void setPublishResults(PublishedItem[] publishResults) {
        this.publishResults = publishResults;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPublishStartDate() {
        return publishStartDate;
    }

    public void setPublishStartDate(String publishStartDate) {
        this.publishStartDate = publishStartDate;
    }
}
