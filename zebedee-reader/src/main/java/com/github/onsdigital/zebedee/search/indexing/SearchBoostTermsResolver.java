package com.github.onsdigital.zebedee.search.indexing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.elasticSearchLog;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.trim;

/**
 * Created by bren on 25/01/16.
 */
public class SearchBoostTermsResolver {

    //terms that are mapped only to given uri
    private static Map<String, List<String>> singleMapTerms = new HashMap<>();
    //terms that are mapped to all documents with uris starting with map's key
    private static Map<String, List<String>> prefixMapTerms = new HashMap<>();

    private static SearchBoostTermsResolver instance = new SearchBoostTermsResolver();

    private SearchBoostTermsResolver() {
    }

    public static void loadTerms() throws IOException {
        loadTerms("/search/boost.txt");
    }


    static void loadTerms(String fileName) throws IOException {
        try (
                InputStream resourceStream = SearchBoostTermsResolver.class.getResourceAsStream(fileName);
                InputStreamReader inputStreamReader = new InputStreamReader(resourceStream);
                BufferedReader br = new BufferedReader(inputStreamReader)
        ) {
            for (String line; (line = br.readLine()) != null; ) {
                processMapping(line);
            }
        }
    }

    public static SearchBoostTermsResolver getSearchTermResolver() {
        return instance;
    }

    public List<String> getTerms(String uri) {
        return singleMapTerms.get(uri);
    }

    public List<String> getTermsForPrefix(String uri) {
        return prefixMapTerms.get(uri);
    }


    private static void processMapping(String line) {
        if (isEmpty(line) || startsWith(line, "#")) {
            return; // skip comments
        }

        String[] split = line.split(" *=> *");
        if (split.length != 2) {
            elasticSearchLog("Skipping invalid search boost mapping").addParameter("line", line).log();
        }
        String uri = split[0];
        String[] terms = split[1].split(" *, *");
        uri = trim(uri);
        if (terms == null || terms.length == 0) {
            return;
        }
        if (endsWith(uri, "*")) {
            prefixMapTerms.put(removeEnd(removeEnd(uri, "*"), "/"), asList(terms));
        } else {
            singleMapTerms.put(removeEnd(uri, "/"), asList(terms));
        }
    }


}
