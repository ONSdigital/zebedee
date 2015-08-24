package com.github.onsdigital.zebedee.search.result;

import java.util.List;

public class SearchResults {

    private long numberOfResults;
    private List<SearchResult> results;

    public long getNumberOfResults() {
        return numberOfResults;
    }

    public void setNumberOfResults(long numberOfResults) {
        this.numberOfResults = numberOfResults;
    }

    public List<SearchResult> getResults() {
        return results;
    }

    public void setResults(List<SearchResult> results) {
        this.results = results;
    }

}
