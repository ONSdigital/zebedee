package com.github.onsdigital.zebedee.search.indexing;

public enum Index {
    DEPARTMENTS("departments"),
    ONS("ons");

    private String index;

    Index(String index) {
        this.index = index;
    }

    public String getIndex() {
        return index;
    }
}
