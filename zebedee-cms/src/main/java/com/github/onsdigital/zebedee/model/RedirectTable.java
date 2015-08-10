package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.content.page.base.Page;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class RedirectTable {
    private HashMap<String, String> table = new HashMap<>();
    private RedirectTable child = null;
    private Content content = null;
    private final int ITERATION_MAX = 100; // Simple method to avoid cycling

    public RedirectTable(Content content) {
        this.content = content;
    }

    public RedirectTable(Content content, Path path) throws IOException {
        this(content);
        loadFromPath(path);
    }

    /**
     * Child allows us to chain redirect tables
     *
     * This allow with redirects in Florence since we can have an 'inprogress' table (the parent)
     * in tandem a 'published' table (the child)
     *
     * During publication we can merge chained redirects
     *
     * @param child a secondary
     */
    public void setChild(RedirectTable child) {
        this.child = child;
    }
    public RedirectTable getChild() {
        return child;
    }

    /**
     *
     * @param redirectFrom original uri
     * @param redirectTo redirect uri
     */
    public void addRedirect(String redirectFrom, String redirectTo) {
        table.put(redirectFrom, redirectTo);
    }
    public void removeRedirect(String redirectFrom) {
        table.remove(redirectFrom);
    }

    /**
     *
     * @param uri the requested uri
     *
     * @return the redirected uri
     */
    public String get(String uri) {
        String finalUriAtThisLevel = endChain(uri, ITERATION_MAX);        // Follow redirect chain
        if (finalUriAtThisLevel == null) { return null; }       // Check for cyclical

        if (content.exists(finalUriAtThisLevel, false)) {              // Option 1) Uri exists - return it
            return finalUriAtThisLevel;
        } else if (child != null) {                             //
            String chained = child.get(finalUriAtThisLevel);
            if (chained != null) {                              // Option 2a) Child level - continue chain
                return chained;
            } else {
                return child.get(uri);                          // Option 2b) Child level - try from scratch
            }
        } else {
            return null;
        }
    }

    private String endChain(String uri, int iterations) {
        if (iterations == 0) { return null; } // checks we haven't cycled

        if (!content.exists(uri, false) && table.containsKey(uri)) {
            return endChain(table.get(uri), --iterations);
        }
        return uri;
    }

    /**
     * Pull a chain of child 301 tables into the parent
     */
    public void merge() {
        if (child != null) {
            child.merge();
            merge(child);
            child = null;
        }
    }
    void merge(RedirectTable redirect) {
        for (String key: redirect.table.keySet()) {
            if (!table.containsKey(key)) {
                table.put(key, redirect.table.get(key));
            }
        }
    }

    public void saveToPath(Path path) throws IOException {
        // TODO quite a considerable amount of threadsafe making
        try (FileWriter stream = new FileWriter(path.toFile()); BufferedWriter out = new BufferedWriter(stream)) {
            for (String fromUri : table.keySet()) {
                String toUri = table.get(fromUri);
                out.write(fromUri + '\t' + toUri);
                out.newLine();
            }
        }
    }

    public void loadFromPath(Path path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                // process the line.
                String[] fromTo = line.split("\t");
                if (fromTo.length > 1) {
                    table.put(fromTo[0], fromTo[1]);
                }
            }
        }
    }
}
