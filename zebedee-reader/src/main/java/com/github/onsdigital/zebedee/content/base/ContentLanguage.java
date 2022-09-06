package com.github.onsdigital.zebedee.content.base;

import java.util.stream.Stream;

public enum ContentLanguage {
    ENGLISH("en"), WELSH("cy");

    private final static String DATA_FILE_NAME = "data%s.json";

    public static ContentLanguage getById(final String id) {
        return Stream.of(ContentLanguage.values()).filter(l -> l.getId().equalsIgnoreCase(id)).findAny().get();
    }

    private final String id;

    ContentLanguage(String id) {
        this.id = id;
    }

    public String getDataFileName() {
        return String.format(DATA_FILE_NAME, getFileSuffix());
    }

    public String getId() {
        return id;
    }

    public String getFileSuffix() {
        switch (this) {
        case ENGLISH:
            // No suffix for English
            return "";
        default:
            return "_" + id;
        }
    }

    @Override
    public String toString() {
        return id;
    }
}
