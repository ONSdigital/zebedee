package com.github.onsdigital.zebedee.model;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Partial redirects work by matching as much of the URI as possible to the redirect table and forwarding accordingly
 *
 * This optimises storage since we don't need to have 60 entries for all
 */
public class RedirectTablePartialMatch implements RedirectTable {
    Content content;
    Map<String, List<String>> table = new ConcurrentHashMap<>(); // Concurrent is going to be necessary for save and add

    public RedirectTablePartialMatch(Content content) {
        this.content = content;
    }

    public RedirectTablePartialMatch(Content content, Path path) {
        this.content = content;
    }

    @Override
    public String get(String uri) {
        if (content.exists(uri, false)) { return uri; } // most times the URI will exist

        // Otherwise lets chuck iteration and go for a bit of dynamic programming
        Path path = Paths.get(uri);
        for(int i = path.getNameCount() - 1; i >= 0; i--){
            // Reverse down the length of the uri path
            String partialFrom = path.subpath(0, i + 1).toString();

            // See if this brings up a match
            if (table.containsKey(partialFrom)) {
                List<String> options = table.get(partialFrom);
                for (String partialTo: options) {
                    String redirected = partialTo + uri.substring(partialFrom.length());
                    if (content.exists(redirected, false)) { return redirected; }
                }
            }
        }

        // No matches at all
        return null;
    }

    @Override
    public void addRedirect(String redirectFrom, String redirectTo) {
        // Check the redirect doesn't already exist
        if (this.table.containsKey(redirectFrom)) {
            for (String partialTo: this.table.get(redirectFrom)) {
                if (partialTo.equalsIgnoreCase(redirectTo)) { return;}
            }
        }

        // Create a clone
        Map<String, List<String>> newTable = new ConcurrentHashMap<>();

        // Update the existing table
        for (String partialFrom: this.table.keySet()) {

            // At each node we start to build a new list
            List<String> newList = new ArrayList<>();

            // We iterate through the current list
            List<String> list = table.get(partialFrom);
            for (String partialTo: list) {
                // If the new redirect affects the to value we change it
                if (partialTo.startsWith(redirectFrom)) {
                    String newLink = redirectTo + partialTo.substring(redirectFrom.length());
                    newList.add(newLink);
                } else { // or just copy the link
                    newList.add(partialTo);
                }
            }
            newTable.put(partialFrom, newList);
        }

        // Now add the link (we know it doesn't exist)
        if (!newTable.containsKey(redirectFrom)) { newTable.put(redirectFrom, new ArrayList<String>()); }
        newTable.get(redirectFrom).add(redirectTo);

        // replace the class level table
        table = newTable;
    }

    @Override
    public void removeRedirect(String redirectFrom, String redirectTo) {
        // Create a clone
        Map<String, List<String>> newTable = new ConcurrentHashMap<>();

        // We iterate through the table
        for (String partialFrom: this.table.keySet()) {

            // At each node we start to build a new list
            List<String> newList = new ArrayList<>();

            // We iterate through the current list
            List<String> list = table.get(partialFrom);
            for (String partialTo: list) {
                // For all non matching
                if (!(redirectFrom.equalsIgnoreCase(partialFrom) && redirectTo.equalsIgnoreCase(partialTo))) {
                    newList.add(partialTo);
                }
            }
            newTable.put(partialFrom, newList);
        }

        table = newTable;
    }

    @Override
    public void merge(RedirectTable redirectTable) {
        Iterator<String[]> pairs = redirectTable.iterator();

        while( pairs.hasNext() ) {

            String[] fromTo = pairs.next();
            addRedirect(fromTo[0], fromTo[1]);
        }
    }

    @Override
    public void save(Path path) throws IOException {
        // TODO quite a considerable amount of threadsafe making
        try (FileWriter stream = new FileWriter(path.toFile()); BufferedWriter out = new BufferedWriter(stream)) {
            for (String[] fromTo : this) {
                out.write(fromTo[0] + '\t' + fromTo[1]);
                out.newLine();
            }
        }
    }

    @Override
    public void load(Path path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                // process the line.
                String[] fromTo = line.split("\t");
                addRedirect(fromTo[0], fromTo[1]);
            }
        }
    }

    @Override
    public boolean exists(String redirectFrom, String redirectTo) {
        if (table.containsKey(redirectFrom) && table.get(redirectFrom).contains(redirectTo)) { return true; }
        return false;
    }

    @Override
    public Iterator<String[]> iterator() {
        Iterator<String[]> it = new Iterator<String[]>() {

            private Iterator<String> keyset = table.keySet().iterator();
            private Iterator<String> toSet;
            private String key = null;

            @Override
            public boolean hasNext() {
                return (keyset.hasNext() || (toSet != null && toSet.hasNext()));
            }

            @Override
            public String[] next() {
                // go to next from key if necessary
                if ((toSet == null || toSet.hasNext() == false) && keyset.hasNext()) {
                    key = keyset.next();
                    toSet = table.get(key).iterator();
                }

                // return next on the 'to' list

                String[] fromTo = new String[2];

                fromTo[0] = key;
                fromTo[1] = toSet.next();
                return fromTo;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return it;
    }


}
