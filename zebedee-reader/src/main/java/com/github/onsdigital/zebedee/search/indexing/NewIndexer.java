package com.github.onsdigital.zebedee.search.indexing;

import com.github.onsdigital.zebedee.search.indexing.content.NodeClientIndexer;
import com.github.onsdigital.zebedee.search.indexing.content.ZebedeeContentIndexer;
import org.elasticsearch.action.index.IndexResponse;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;

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
    public void reindex() {
        if (this.lock.tryLock()) {
            try {
                // Lock in cluster
                this.lockGlobal();

                this.indexContent();

            } finally {
                this.lock.unlock();
                this.indexClient.deleteGlobalLockDocument();
            }
        }
        throw new IndexInProgressException();
    }

    private void indexContent() {
        this.contentIndexer.indexDepartments();
        this.contentIndexer.indexOnsContent();
    }

    /**
     * Acquires a global index lock
     */
    private void lockGlobal() {
        IndexResponse lockResponse = this.indexClient.createGlobalLockDocument();
        if (!lockResponse.isCreated()) {
            IndexInProgressException indexInProgressException = new IndexInProgressException();
            logError(indexInProgressException).log();
            throw indexInProgressException;
        }
    }


    public static void main(String[] args) {
        // Main method to trigger reindex
        NewIndexer.getInstance().reindex();
    }

}
