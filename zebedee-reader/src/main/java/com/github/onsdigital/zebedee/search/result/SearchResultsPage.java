package com.github.onsdigital.zebedee.search.result;


import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.taxonomy.TaxonomyProductNode;

import java.util.List;

/**
 * Created by bren on 29/06/15.
 */
public class SearchResultsPage extends Page {

    private SearchResults taxonomySearchResult;
    private SearchResults statisticsSearchResult;
    private String searchTerm;
    //When search is autocorrected
    private boolean suggestionBased;
    private String suggestion;
    private int currentPage;
    private int startPage;
    private int endPage;
    private List<Integer> pages;
    private long numberOfResults;
    private TaxonomyProductNode headlinePage;
    private long numberOfPages;
    private String[] types;
    private boolean includeAllData;
    private boolean includeStatics;


    public SearchResults getTaxonomySearchResult() {
        return taxonomySearchResult;
    }

    public void setTaxonomySearchResult(SearchResults taxonomySearchResult) {
        this.taxonomySearchResult = taxonomySearchResult;
    }

    public SearchResults getStatisticsSearchResult() {
        return statisticsSearchResult;
    }

    public void setStatisticsSearchResult(SearchResults statisticsSearchResult) {
        this.statisticsSearchResult = statisticsSearchResult;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public boolean isSuggestionBased() {
        return suggestionBased;
    }

    public void setSuggestionBased(boolean suggestionBased) {
        this.suggestionBased = suggestionBased;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    @Override
    public PageType getType() {
        return PageType.search_results_page;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public long getNumberOfResults() {
        return numberOfResults;
    }

    public void setNumberOfResults(long numberOfResults) {
        this.numberOfResults = numberOfResults;
    }

    public TaxonomyProductNode getHeadlinePage() {
        return headlinePage;
    }

    public void setHeadlinePage(TaxonomyProductNode headlinePage) {
        this.headlinePage = headlinePage;
    }

    public long getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(long numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public String[] getTypes() {
        return types;
    }

    public void setTypes(String[] types) {
        this.types = types;
    }

    public boolean isIncludeStatics() {
        return includeStatics;
    }

    public void setIncludeStatics(boolean includeStatics) {
        this.includeStatics = includeStatics;
    }

    public int getStartPage() {
        return startPage;
    }

    public void setStartPage(int startPage) {
        this.startPage = startPage;
    }

    public int getEndPage() {
        return endPage;
    }

    public void setEndPage(int endPage) {
        this.endPage = endPage;
    }

    public List<Integer> getPages() {
        return pages;
    }

    public void setPages(List<Integer> pages) {
        this.pages = pages;
    }

    public boolean isIncludeAllData() {
        return includeAllData;
    }

    public void setIncludeAllData(boolean includeAllData) {
        this.includeAllData = includeAllData;
    }
}
