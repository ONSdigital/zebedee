package com.github.onsdigital.zebedee.model;

import java.util.HashMap;

/**
 * Created by thomasridd on 04/08/15.
 */
public class RedirectTable {
    private HashMap<String, String> table = new HashMap<>();
    private RedirectTable child = null;
    private Content content = null;

    public RedirectTable(Content content) {
        this.content = content;
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

     * @return
     */
    public String get(String uri) {
        String finalUriAtThisLevel = endChain(uri);             // Follow redirect chain

        if (content.exists(finalUriAtThisLevel)) {              // Option 1) Uri exists - return it
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
//        if (content.exists(uri)) {          // If content exists return the uri
//            return uri;
//        } else if (table.containsKey(uri)) {    // If a link exists follow the link
//            String nextUri = table.get(uri);
//            if (content.exists(nextUri)) {
//                return child.get(uri);
//            } else {
//                return get(table.get(uri));
//            }
//        } else if (child != null) {
//            // Alternatively check for a redirect link
//
//            if (nextUri != null) {
//                return get(nextUri);            // Look for content at the redirect (recursive)
//            } else {
//                return null;                    // return null
//            }
//        }
    }
    private String endChain(String uri) {
        if (content.exists(uri) == false && table.containsKey(uri)) {
           return endChain(table.get(uri));
        }
        return uri;
    }




//    String get(String uri) {
//        if (table.containsKey(uri)) {   // Look for uri in own table
//            String newUri = table.get(uri);
//            if (newUri != null) {
//                return newUri;
//            } else if (child != null) { // If the parent link is invalid drop to the child
//                return child.get(uri);
//            }
//        } else if (child != null) {     // Try children
//            return child.get(uri);
//        }
//        return null;                    // return null
//    }


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
}
