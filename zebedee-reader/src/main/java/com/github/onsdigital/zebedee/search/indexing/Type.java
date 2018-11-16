package com.github.onsdigital.zebedee.search.indexing;

public enum Type {

    DEFAULT("_default_"),
    DEPARTMENTS("departments");

    private String type;

    Type(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
