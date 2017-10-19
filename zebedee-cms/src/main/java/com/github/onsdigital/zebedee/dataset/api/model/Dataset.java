package com.github.onsdigital.zebedee.dataset.api.model;

/**
 * The model of a dataset as provided by the dataset API.
 */
public class Dataset {

    private String id;
    private String title;
    private DatasetLinks links;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DatasetLinks getLinks() {
        return links;
    }

    public void setLinks(DatasetLinks links) {
        this.links = links;
    }

    public static class DatasetLinks {
        private Link self;

        public Link getSelf() {
            return self;
        }

        public void setSelf(Link self) {
            this.self = self;
        }
    }
}

