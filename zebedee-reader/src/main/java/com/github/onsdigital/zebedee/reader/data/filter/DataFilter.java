package com.github.onsdigital.zebedee.reader.data.filter;

import java.util.Map;

/**
 * Created by bren on 03/08/15
 *
 *
 * DataFilter is used to filter certain bits of requested json from data endpoint.
 * Not all filters can be applied to all page types
 */
public class DataFilter {


    private FilterType type;
    private Map<String, String[]> parameters;

    public DataFilter(FilterType type) {
        this.type = type;
    }

    public DataFilter(FilterType type, Map<String, String[]> parameters) {
        this(type);
        this.parameters = parameters;

    }

    public FilterType getType() {
        return type;
    }

    public Map<String, String[]> getParameters() {
        return parameters;
    }

    public enum FilterType {
        TITLE, //Denotes only title of content is requested
        DESCRIPTION, //Denotes only title of content is requested
        SERIES; //Denotes time series data is requested. Used for sparklines on website
    }
}
