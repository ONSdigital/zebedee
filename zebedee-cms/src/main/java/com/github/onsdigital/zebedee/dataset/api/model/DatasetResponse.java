package com.github.onsdigital.zebedee.dataset.api.model;

public class DatasetResponse {

    private String id;
    private Dataset current;
    private Dataset next;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Dataset getCurrent() {
        return current;
    }

    public void setCurrent(Dataset current) {
        this.current = current;
    }

    public Dataset getNext() {
        return next;
    }

    public void setNext(Dataset next) {
        this.next = next;
    }
}
