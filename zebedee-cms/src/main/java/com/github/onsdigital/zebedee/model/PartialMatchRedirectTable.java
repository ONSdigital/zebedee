package com.github.onsdigital.zebedee.model;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by thomasridd on 14/08/15.
 */
public class PartialMatchRedirectTable implements RedirectTable {
    Content content;
    Map<String, List<String>> table = new ConcurrentHashMap<>(); // Concurrent is going to be necessary for save and add

    public PartialMatchRedirectTable(Content content) {
        this.content = content;
    }

    @Override
    public String get(String uri) {
        if (content.exists(uri, false)) { return uri; } // most times the URI will exist

        // Otherwise lets chuck iteration and go for a bit of dynamic programming
        Path path = Paths.get(uri);
        for(int i = path.getNameCount() - 2; i >= 0; i--){
            // Reverse down the length of the uri path
            String partialFrom = path.subpath(0, i).toString();

            // See if this brings up a match
            if (table.containsKey(partialFrom)) {
                List<String> options = table.get(partialFrom);
                for (String partialTo: options) {
                    String redirected = partialTo + uri.substring(partialFrom.length());
                    if (content.exists(redirected, false)) { return uri; }
                }
            }
        }

        // No matches at all
        return null;
    }

    @Override
    public void addRedirect(String redirectFrom, String redirectTo) {
        Map<String, List<String>> newTable = new ConcurrentHashMap<>();

        // We iterate through the table
        for (String partialFrom: this.table.keySet()) {

            // At each node we start to build a new list
            List<String> newList = new ArrayList<>();
            List<String> list = table.get(partialFrom);
            for (String partialTo: list) {
                // If the new redirect affects the to value we change it
                if (partialTo.startsWith(redirectFrom)) {
                    String newLink = redirectTo + partialTo.substring(redirectTo.length());

                } else { // or just copy

                }
            }
        }
    }

    @Override
    public void removeRedirect(String redirectFrom) {

    }

    @Override
    public void save(Path path) throws IOException {

    }

    @Override
    public void load(Path path) throws IOException {

    }
}
