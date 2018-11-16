package com.github.onsdigital.zebedee.search.indexing;

import com.github.onsdigital.zebedee.search.indexing.content.NodeClientIndexer;
import com.github.onsdigital.zebedee.search.indexing.content.ZebedeeContentIndexer;

/**
 * This class is now just a wrapper around the desired ZebedeeContentIndexer instance
 */
public class Indexer {

    private static Indexer INSTANCE;

    public static Indexer getInstance() {
        if (INSTANCE == null) {
            synchronized (Indexer.class) {
                INSTANCE = new Indexer();
            }
        }
        return INSTANCE;
    }

    private final ZebedeeContentIndexer contentIndexer;

    private Indexer() {
        this.contentIndexer = new NodeClientIndexer();
    }

    /**
     * Reindex all pages under a given uri
     * @param uri
     */
    public void reindexByUri(String uri) {
        this.contentIndexer.indexByUri(uri);
    }

    public void reindex() {
        this.contentIndexer.reindex();
    }


    public static void main(String[] args) {
        // Main method to trigger reindex
        Indexer.getInstance().reindex();
    }

}
