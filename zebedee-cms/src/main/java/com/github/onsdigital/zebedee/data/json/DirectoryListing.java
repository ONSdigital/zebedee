package com.github.onsdigital.zebedee.data.json;


import java.lang.String;import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a directory listing within the site.
 *
 * @author david
 *
 */
public class DirectoryListing {

    public Map<String, String> folders = new HashMap<>();
    public Map<String, String> files = new HashMap<>();
}