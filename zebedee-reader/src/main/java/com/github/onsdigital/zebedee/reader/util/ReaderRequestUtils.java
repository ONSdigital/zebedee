package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.reader.data.filter.DataFilter;
import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by bren on 20/08/15.
 */
public class ReaderRequestUtils {

    /**
     * Gets lang parameter passed with request
     *
     * @return
     */
    public static ContentLanguage getRequestedLanguage(HttpServletRequest request) {
        String lang = request.getParameter("lang");
        if (lang == null) {
            return null;
        }

        try {
            return ContentLanguage.valueOf(lang);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }


    /**
     * Extracts the filter requested to be applied to content. Each request should have at most one filter, otherwise first filter found will be applied
     *
     * @param request
     * @return null if no filter requested
     */
    public static DataFilter extractFilter(HttpServletRequest request) throws UnsupportedEncodingException {
        Set<Map.Entry<String, String[]>> entries = request.getParameterMap().entrySet();
        for (Iterator<Map.Entry<String, String[]>> iterator = entries.iterator(); iterator.hasNext(); ) {
            Map.Entry<String, String[]> param = iterator.next();
            String key = param.getKey();
            try {
                DataFilter.FilterType filterType = DataFilter.FilterType.valueOf(key.toUpperCase());
                return new DataFilter(filterType,getQueryParameters(request));
            } catch (IllegalArgumentException ie) { //Not a filter parameter
                continue;
            }
        }
        return null;
    }


    /**
     * Extracts GET parameters from query string
     * <p/>
     * This method matches parameters to query string, if parameters is in query string it is return in the list of parameters.
     * <p/>
     * Note that a post parameters with the same name might also be included. There should not be parameters with same names in both get and post parameters if not wanted to be extracted
     */
    public static Map<String, String[]> getQueryParameters(HttpServletRequest request) throws UnsupportedEncodingException {
        Map<String, String[]> queryParameters = new HashMap<>();

        if (request == null || request.getQueryString() == null ||
                request.getQueryString().length() == 0) {
            return queryParameters;
        }

        String queryString = URLDecoder.decode(request.getQueryString(), StandardCharsets.UTF_8.name());

        if (StringUtils.isEmpty(queryString)) {
            return queryParameters;
        }

        String[] parameters = queryString.split("&");

        for (String parameter : parameters) {
            String[] keyValuePair = parameter.split("=");
            String[] values = queryParameters.get(keyValuePair[0]);
            values = ArrayUtils.add(values, keyValuePair.length == 1 ? "" : keyValuePair[1]); //length is one if no value is available.
            queryParameters.put(keyValuePair[0], values);
        }
        return queryParameters;
    }
}
