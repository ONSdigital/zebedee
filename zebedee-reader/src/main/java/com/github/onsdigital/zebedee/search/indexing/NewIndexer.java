package com.github.onsdigital.zebedee.search.indexing;

import com.github.onsdigital.zebedee.search.configuration.SearchConfiguration;
import com.github.onsdigital.zebedee.search.indexing.content.NodeClientIndexer;
import com.github.onsdigital.zebedee.search.indexing.content.ZebedeeContentIndexer;
import org.elasticsearch.action.index.IndexResponse;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NewIndexer {

    private static NewIndexer INSTANCE;

    public static NewIndexer getInstance() {
        if (INSTANCE == null) {
            synchronized (NewIndexer.class) {
                INSTANCE = new NewIndexer();
            }
        }
        return INSTANCE;
    }

    private final IndexClient indexClient;
    private final ZebedeeContentIndexer contentIndexer;
    private final Lock lock;

    private NewIndexer() {
        this.indexClient = IndexClient.getInstance();
        this.contentIndexer = new NodeClientIndexer();
        this.lock = new ReentrantLock();
    }

    /**
     * Triggers a complete reindex of Elasticsearch
     */
    public void reindex() throws IOException {
        if (this.lock.tryLock()) {
            try {
                // Lock in cluster
                this.lockGlobal();

                // Get the search alias
                String searchAlias = SearchConfiguration.getSearchAlias();

                // Check if the index alias is available
                boolean aliasIsAvailable = this.indexClient.indexExists(searchAlias);

                // Get the old index from the current alias
                String oldIndex = this.indexClient.getIndexForAlias(searchAlias);

                // Generate new index name
                String newIndex = this.generateIndexName();

                // Create the new index
                this.indexClient.createIndex(newIndex, ZebedeeContentIndexer.getSettings(), ZebedeeContentIndexer.getDefaultMapping());

                // Process the possible scenarious
                if (aliasIsAvailable && oldIndex == null) {
                    //In this case it is an index rather than an alias. This normally is not possible with index structure set up.
                    //This is a transition code due to elastic search index structure change, making deployment to environments with old structure possible without down time
                    this.indexClient.deleteIndex(searchAlias);
                }

                if (oldIndex == null) {
                    this.indexClient.addIndexAlias(newIndex, searchAlias);
                    this.indexContent(newIndex);
                } else {
                    this.indexContent(newIndex);
                    this.indexClient.swapIndexAlias(oldIndex, newIndex, searchAlias);
                    this.indexClient.deleteIndex(oldIndex);
                }

            } finally {
                this.lock.unlock();
                this.indexClient.deleteGlobalLockDocument();
            }
        }
        throw new IndexInProgressException();
    }

    private void indexContent(String onsIndexName) {
        this.contentIndexer.indexDepartments();
        this.contentIndexer.indexOnsContent(onsIndexName);
    }

    private String generateIndexName() {
        return SearchConfiguration.getSearchAlias() + System.currentTimeMillis();
    }

    /**
     * Acquires a global index lock
     */
    private void lockGlobal() {
        IndexResponse lockResponse = this.indexClient.createGlobalLockDocument();
        if (!lockResponse.isCreated()) {
            throw new IndexInProgressException();
        }
    }

}
