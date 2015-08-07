package com.github.onsdigital.zebedee.reader.data.filter;

/**
 * Created by bren on 03/08/15
 *
 *
 * DataFilter is used to filter certain bits of requested json from data endpoint.
 * Not all filters can be applied to all page types
 */
public enum DataFilter {
    TITLE, //Denotes only title of content is requested
    DESCRIPTION, //Denotes only title of content is requested
    SERIES; //Denotes time series data is requested. Used for sparklines on website

}
