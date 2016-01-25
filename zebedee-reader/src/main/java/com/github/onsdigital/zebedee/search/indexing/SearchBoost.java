package com.github.onsdigital.zebedee.search.indexing;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * Created by bren on 25/01/16.
 */
public class SearchBoost {

    //terms that are mapped only to given uri
    private static Map<String, String[]> singleMapTerms;

    //terms that are mapped to all documents with uris starting with map's key
    private static Map<String, String[]> prefixMapTerms;


    public static void init() throws IOException {
        if (singleMapTerms == null) {
            synchronized (SearchBoost.class) {
                if (singleMapTerms == null) {
                    readFile("boost.txt");
                }
            }
        }
    }

    static void readFile(String fileName) throws IOException {
        singleMapTerms = new HashMap<>();
        prefixMapTerms = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(SearchBoost.class.getResourceAsStream(fileName)))) {
            for (String line; (line = br.readLine()) != null; ) {
                processMapping(line);
            }
        }

    }

    public static String[] getTerms(String uri) {
        return singleMapTerms.get(uri);
    }

    public static String[] getTermsForPrefix(String uri) {
        return prefixMapTerms.get(uri);
    }


    private static void processMapping(String line) {
        if (isEmpty(line) || startsWith(line, "#")) {
            return; // skip comments
        }

        String[] split = line.split(" *=> *");
        if (split.length != 2) {
            System.out.println("Skipping invalid search boost mapping. line: " + line);
        }
        String uri = split[0];
        String[] terms = split[1].split(" *, *");
        uri = trim(uri);
        if (endsWith(uri, "*")) {
            prefixMapTerms.put(removeEnd(removeEnd(uri, "*"), "/"), terms);
        } else {
            singleMapTerms.put(removeEnd(uri, "/"), terms);
        }
    }


}
