package com.github.onsdigital.zebedee.model;
import java.util.HashMap;

/**
 * Created by thomasridd on 04/08/15.
 */
public class Zebedee301 {
    private HashMap<String, String> table = new HashMap<>();
    private Zebedee301 child = null;

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
    public void setChild(Zebedee301 child) {
        this.child = child;
    }
    public Zebedee301 getChild() {
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
     * @param content a content object to pull from
     * @return
     */
    public String get(String uri, Content content) {
        if (content.exists(uri)) {              // If content exists return the uri
            return uri;
        } else {                                // Alternatively check for a redirect link
            String nextUri = get(uri);
            if (nextUri != null) {
                return get(nextUri, content);   // Look for content at the redirect (recursive)
            } else {
                return null;                    // return null
            }
        }
    }

    String get(String uri) {
        if (table.containsKey(uri)) {   // Look for uri in own table
            return table.get(uri);
        } else if (child != null) {     // Try children
            return child.get(uri);
        } else {                        // return null
            return null;
        }
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
    void merge(Zebedee301 redirect) {
        for (String key: redirect.table.keySet()) {
            if (!table.containsKey(key)) {
                table.put(key, redirect.table.get(key));
            }
        }
    }
}
