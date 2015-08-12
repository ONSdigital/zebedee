package com.github.onsdigital.zebedee.search.response;

import com.github.onsdigital.zebedee.content.page.base.Page;

import java.util.List;

public class SearchResult {

    private long numberOfResults;
    private List<Page> results;

    public long getNumberOfResults() {
        return numberOfResults;
    }

    public void setNumberOfResults(long numberOfResults) {
        this.numberOfResults = numberOfResults;
    }

    public List<Page> getResults() {
        return results;
    }

    public void setResults(List<Page> results) {
        this.results = results;
    }

}
