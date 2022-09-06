package com.github.onsdigital.zebedee.reader.data.language;

/**
 * Created by bren on 19/08/15.
 */
public enum ContentLanguage {
    ENGLISH("en", "data.json"),
    WELSH("cy", "data_cy.json");

    private final String id;
    private final String dataFileName;

    ContentLanguage(String id, String dataFileName) {
        this.id = id;
        this.dataFileName = dataFileName;
    }

    public String getDataFileName() {
        return dataFileName;
    }

    public String getId() {
        return id;
    }
    
    @Override
    public String toString() {
        return id;
    }
}
