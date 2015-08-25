package com.github.onsdigital.zebedee.search.api.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;

public class RequestUtil {

    /**
     * Extract the page number from a request - for paged results.
     *
     * @param request
     * @return
     */
    public static int extractPage(HttpServletRequest request) {
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

    public static String[] extractTypes(HttpServletRequest request) {
        String[] types = request.getParameterValues("type");
        return ArrayUtils.isNotEmpty(types) ? types : null;
    }
}
