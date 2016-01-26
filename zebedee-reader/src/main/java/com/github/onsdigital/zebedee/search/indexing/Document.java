package com.github.onsdigital.zebedee.search.indexing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by bren on 25/01/16.
 */
public class Document {
    private String uri;
    private List<String> searchTerms;

    public Document(String uri,  Set<List<String>> searchTerms) {
        this.uri = uri;
        this.searchTerms = resovleSearchTerms(searchTerms);
    }

    private List<String> resovleSearchTerms(Set<List<String>> terms) {
        if (terms == null) {
            return null;
        }
        List<String> searchTerms = new ArrayList<>();
        for (List<String> termList : terms) {
            if (termList == null) {
                continue;
            }
            for (String term : termList) {
                searchTerms.add(term);
            }
        }
        return searchTerms;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public List<String> getSearchTerms() {
        return searchTerms;
    }

    public void setSearchTerms(List<String> searchTerms) {
        this.searchTerms = searchTerms;
    }
}
