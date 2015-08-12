package com.github.onsdigital.zebedee.search.response;

import com.github.onsdigital.zebedee.content.page.base.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents results aggregated together to be serialized into JSON
 *
 * @author brn
 */
public class AggregatedSearchResult {

    // Search result of home type pages
    public SearchResult taxonomySearchResult;
    public SearchResult statisticsSearchResult;
    public long timeseriesCount;
    private boolean suggestionBasedResult;
    private String suggestion;

    public boolean isSuggestionBasedResult() {
        return suggestionBasedResult;
    }

    public void setSuggestionBasedResult(boolean suggestionBasedResult) {
        this.suggestionBasedResult = suggestionBasedResult;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public long getNumberOfResults() {
        long numberOfResults = statisticsSearchResult.getNumberOfResults();
        if (taxonomySearchResult != null) {
            return numberOfResults += taxonomySearchResult.getNumberOfResults();
        }
        return numberOfResults;

    }

    public List<Page> getAllResults() {
        List<Page> results = new ArrayList<>();
        if (taxonomySearchResult != null) {
            results.addAll(taxonomySearchResult.getResults());
        }
        results.addAll(statisticsSearchResult.getResults());
        return results;
    }

}
