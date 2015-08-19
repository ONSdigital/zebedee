package com.github.onsdigital.zebedee.reader.data.language;

/**
 * Created by bren on 19/08/15.
 */
public enum Language {
    ENGLISH("en"),
    WELSH("cy");

    private final String languageCode;

    Language(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getLanguageCode() {
        return languageCode;
    }
}
