package com.github.onsdigital.zebedee.search.indexing;

/**
 * Created by bren on 04/02/16.
 */
public class Department {

    private String code;
    private String name;
    private String url;
    private String[] terms;

    public Department(String code, String name, String url, String... terms) {
        this.code = code;
        this.name = name;
        this.url = url;
        this.terms = terms;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String[] getTerms() {
        return terms;
    }
}
