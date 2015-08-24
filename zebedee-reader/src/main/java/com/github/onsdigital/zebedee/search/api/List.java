package com.github.onsdigital.zebedee.search.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.search.SearchService;
import com.github.onsdigital.zebedee.search.api.util.RequestUtil;
import com.github.onsdigital.zebedee.search.result.SearchResults;
import com.github.onsdigital.zebedee.search.result.SearchResultsPage;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;

@Api
public class List {
    private final static String HTML_MIME = "text/html";
    private final static String DATA_REQUEST = "data";
    private final static String SEARCH_REQUEST = "search";

    private static final int MAX_VISIBLE_PAGINATOR_LINK = 10;

    @GET
    public Object get(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {

        String uri = request.getParameter("uri");
        int pageNumber = RequestUtil.extractPage(request);
        String[] types = RequestUtil.extractTypes(request);

        SearchResults results = SearchService.list(uri, 10, pageNumber, types);

        long numberOfPages = (long) Math.ceil((double) results.getNumberOfResults() / 10);
        numberOfPages = numberOfPages == 0 ? 1 : numberOfPages; //If no results number should be pages is 1 to show the page
        if (pageNumber > numberOfPages) {
            throw new NotFoundException("There are only " + numberOfPages + " available - page " + pageNumber + " does not exist.");
        }

        SearchResultsPage page = buildResultsPage(results, pageNumber, "", types);
        page.setUri(new URI(uri));

        String data = ContentUtil.serialise(page);
        IOUtils.copy(new StringReader(data), response.getOutputStream());
        return null;
    }

    //Resolve search headlines and build search page
    private SearchResultsPage buildResultsPage(SearchResults results, int currentPage, String searchTerm, String[] types) {
        SearchResultsPage page = new SearchResultsPage();
        page.setStatisticsSearchResult(results);
        page.setCurrentPage(currentPage);
        page.setNumberOfResults(results.getNumberOfResults());
        page.setNumberOfPages((long) Math.ceil((double) results.getNumberOfResults() / 10));
        page.setEndPage((int) getEndPage(page.getNumberOfPages(), currentPage, MAX_VISIBLE_PAGINATOR_LINK));
        page.setStartPage(getStartPage((int) page.getNumberOfPages(), MAX_VISIBLE_PAGINATOR_LINK, page.getEndPage()));
        page.setPages(getPageList(page.getStartPage(), page.getEndPage()));
        page.setSearchTerm(searchTerm);
        page.setTypes(types);
        return page;
    }

    //
    //List of numbers for handlebars to loop through
    private java.util.List<Integer> getPageList(int start, int end) {
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


}
