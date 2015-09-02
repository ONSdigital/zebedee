package com.github.onsdigital.zebedee.reader.data.language;

/**
 * Created by bren on 19/08/15.
 */
public enum ContentLanguage {
    en("data.json"),
    cy("data_cy.json");

    private final String DATA_FILE_NAME;

    ContentLanguage(String dataFileName) {
        this.DATA_FILE_NAME = dataFileName;
    }

    public String getDataFileName() {
        return DATA_FILE_NAME;
    }
}
