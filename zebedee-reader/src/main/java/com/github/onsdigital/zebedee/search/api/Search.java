package com.github.onsdigital.zebedee.search.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.search.SearchHelper;
import com.github.onsdigital.zebedee.search.result.AggregatedSearchResult;
import com.github.onsdigital.zebedee.search.result.SearchResultsPage;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Api
public class Search {
    private final static String HTML_MIME = "text/html";
    private final static String DATA_REQUEST = "data";
    private final static String SEARCH_REQUEST = "search";

    private static final int MAX_VISIBLE_PAGINATOR_LINK = 10;

    @GET
    public Object get(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {

        String query = extractQuery(request);

        if (query == null) {
            IOUtils.copy(new StringReader(ContentUtil.serialise(new SearchResultsPage())), response.getOutputStream());
            return null;
        }

        Object searchResult = null;
        int page = extractPage(request);
        String[] types = extractTypes(request);
        boolean includeStatics = "1".equals(request.getParameter("includeStatics"));
        boolean includeAllData = "1".equals(request.getParameter("includeAllData"));
        String[] filterTypes = resolveTypes(types, includeStatics, includeAllData); //Not changing original type request as it is modified here and should remain same for the page
        searchResult = search(query, page, filterTypes);
        if (searchResult == null) {
            System.out.println("Attempting search against timeseries as no results found for: " + query);
            URI timeseriesUri = searchTimseries(query);
            if (timeseriesUri == null) {
                System.out.println("No results found from timeseries so using suggestions for: " + query);
                searchResult = searchAutocorrect(query, page, filterTypes);
            } else {
                response.sendRedirect(timeseriesUri.toString());
                return null;
            }
        }

            /*else if (StringUtils.isNotBlank(request.getParameter("term"))) {
                searchResult = autoComplete(query);
            } else if (StringUtils.isNotBlank(request.getParameter("cdid"))) {
                URI timeseriesUri = SearchHelper.searchCdid(query);
                return timeseriesUri == null ? "" : timeseriesUri;
            }*/


        AggregatedSearchResult result = (AggregatedSearchResult) searchResult;
        long numberOfPages = (long) Math.ceil((double) result.statisticsSearchResult.getNumberOfResults() / 10);
        numberOfPages = numberOfPages == 0 ? 1 : numberOfPages; //If no results number should be pages is 1 to show the page
        if (page > numberOfPages) {
            throw new NotFoundException("There are only " + numberOfPages + " available - page " + page + " does not exist.");
        }

        String data = ContentUtil.serialise(buildResultsPage(result, page, query, types, includeStatics, includeAllData));
        IOUtils.copy(new StringReader(data), response.getOutputStream());
        return null;
    }


    private String[] resolveTypes(String[] types, boolean includeStatics, boolean includeAllData) {
        String[] filterTypes = ArrayUtils.clone(types);
        if (includeStatics) {
            filterTypes = ArrayUtils.add(filterTypes, PageType.static_adhoc.name());
            filterTypes = ArrayUtils.add(filterTypes, PageType.static_page.name());
            filterTypes = ArrayUtils.add(filterTypes, PageType.static_article.name());
            filterTypes = ArrayUtils.add(filterTypes, PageType.static_foi.name());
        }
        if (includeAllData) {
            filterTypes = ArrayUtils.add(filterTypes, PageType.timeseries.name());
        }

        return filterTypes;
    }


    //Resolve search headlines and build search page
    private SearchResultsPage buildResultsPage(AggregatedSearchResult results, int currentPage, String searchTerm, String[] types, boolean includeStatics, boolean includeAllData) {
        SearchResultsPage page = new SearchResultsPage();
        page.setStatisticsSearchResult(results.statisticsSearchResult);
        page.setTaxonomySearchResult(results.taxonomySearchResult);
        page.setCurrentPage(currentPage);
        page.setIncludeStatics(includeStatics);
        page.setIncludeAllData(includeAllData);
        page.setNumberOfResults(results.getNumberOfResults());
        page.setNumberOfPages((long) Math.ceil((double) results.statisticsSearchResult.getNumberOfResults() / 10));
        page.setEndPage((int) getEndPage(page.getNumberOfPages(), currentPage, MAX_VISIBLE_PAGINATOR_LINK));
        page.setStartPage(getStartPage((int) page.getNumberOfPages(), MAX_VISIBLE_PAGINATOR_LINK, page.getEndPage()));
        page.setPages(getPageList(page.getStartPage(), page.getEndPage()));
        page.setSearchTerm(searchTerm);
        page.setTypes(types);
        page.setSuggestionBased(results.isSuggestionBasedResult());
        if (results.isSuggestionBasedResult()) {
            page.setSuggestion(results.getSuggestion());
        }

        if (page.getTaxonomySearchResult() != null) {
            // todo: resolve search headline in here or in babbage ?
            //resolveSearchHeadline(page);
        }
        return page;
    }

    //
    //List of numbers for handlebars to loop through
    private List<Integer> getPageList(int start, int end) {
        ArrayList<Integer> pageList = new ArrayList<Integer>();
        for (int i = start; i <= end; i++) {
            pageList.add(i);
        }
        return pageList;
    }

    //Used for paginator start
    private int getStartPage(int numberOfPages, int maxVisible, int endPage) {
        if (numberOfPages <= maxVisible) {
            return 1;
        }
        int start = endPage - maxVisible + 1;
        start = start > 0 ? start : 1;
        return start;
    }

    private long getEndPage(long numberOfPages, int currentPage, int maxVisible) {
        long max = numberOfPages;
        if (max <= maxVisible) {
            return max;
        }
        //Half of the pages are visible after current page
        long end = (int) (currentPage + Math.ceil(maxVisible / 2));
        end = (end > max) ? max : end;
        end = (end < maxVisible) ? maxVisible : end;
        return end;
    }

    // todo: resolveSearchHeadline
//    private void resolveSearchHeadline(SearchResultsPage page) {
//        for (Iterator<PageReference> iterator = page.getTaxonomySearchResult().getResults().iterator(); iterator.hasNext(); ) {
//            PageReference pageReference = iterator.next();
//            //Beware! Very messy code,
//            // Elastic search does not contain whole data, so we have to load referenced data to get headline data reference (the first data item in the product page) and then data for that page
//            //Afterwards reference needs updating back to data in elastic search to reduce data
//            //Search in general needs tidying up. After going live hopefuly
//
//            if (PageType.product_page == pageReference.getType()) {
//                ContentUtil.loadReferencedPage(LocalFileDataService.getInstance(), pageReference);
//                ProductPage productPage = (ProductPage) pageReference.getData();
//                page.setHeadlinePage(productPage);
//                List<PageReference> items = productPage.getItems();
//                if (items != null) {
//                    if (items.size() > 0) {
//                        PageReference headlineData = items.iterator().next();
//                        if (headlineData != null) {
//                            ContentUtil.loadReferencedPage(LocalFileDataService.getInstance(), headlineData);
//                            iterator.remove();
//                            break;
//                        }
//                    }
//                }
//                break;
//            }
//        }
//    }

    private Object search(String query, int page, String[] types) throws Exception {
        AggregatedSearchResult searchResult = SearchHelper.search(query, page, types);
        if (searchResult.getNumberOfResults() == 0 && types == null) {
            return null;
        }
        return searchResult;
    }

    private URI searchTimseries(String query) {
        return SearchHelper.searchCdid(query);
    }

    private Object searchAutocorrect(String query, int page, String[] types) throws Exception {
        AggregatedSearchResult suggestionResult = SearchHelper.searchSuggestions(query, page, types);
        return suggestionResult;
    }

    public Object autoComplete(String query) {
        return SearchHelper.autocomplete(query);
    }

    private int extractPage(HttpServletRequest request) {
        String page = request.getParameter("page");

        if (StringUtils.isEmpty(page)) {
            return 1;
        }
        if (StringUtils.isNumeric(page)) {
            int pageNumber = Integer.parseInt(page);
            if (pageNumber < 1) {
                return 1;
            }
            return pageNumber;
        } else {
            return 1;
        }
    }

    private String[] extractTypes(HttpServletRequest request) {
        String[] types = request.getParameterValues("type");
        return ArrayUtils.isNotEmpty(types) ? types : null;
    }

    private String extractQuery(HttpServletRequest request) {
        String query = request.getParameter("q");

        if (StringUtils.isEmpty(query)) {
            // check to see if this is part of search's autocomplete
            query = request.getParameter("term");
            if (StringUtils.isEmpty(query)) {
                query = request.getParameter("cdid");
                if (StringUtils.isEmpty(query)) {
                    return null;
                }
            }
        }
        if (query.length() > 200) {
            throw new RuntimeException("Search query contains too many characters");
        }
        String sanitizedQuery = query.replaceAll("[^a-zA-Z0-9 ]+", "");

        return sanitizedQuery;
    }

}
