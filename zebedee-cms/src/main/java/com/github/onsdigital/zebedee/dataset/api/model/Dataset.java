package com.github.onsdigital.zebedee.dataset.api.model;

/**
 * The model of a dataset as provided by the dataset API.
 */
public class Dataset {

    private String id;
    private String title;
    private String collection_id;
    private DatasetLinks links;
    private State state;

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

    public String getCollection_id() {
        return collection_id;
    }

    public void setCollection_id(String collection_id) {
        this.collection_id = collection_id;
    }

    public DatasetLinks getLinks() {
        return links;
    }

    public void setLinks(DatasetLinks links) {
        this.links = links;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
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

