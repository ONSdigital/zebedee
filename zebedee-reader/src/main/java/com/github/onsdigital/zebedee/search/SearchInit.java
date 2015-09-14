package com.github.onsdigital.zebedee.search;

import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import com.github.onsdigital.zebedee.search.indexing.Indexer;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by bren on 03/09/15.
 * <p>
 * Search module initialization entry point
 */
public class SearchInit implements Startup {
    @Override
    public void init() {
        ElasticSearchClient.init();
        loadIndex();

    }

    private void loadIndex() {
        final ExecutorService thread = Executors.newSingleThreadExecutor();
        thread.submit(new Callable() {
                          @Override
                          public Object call() throws Exception {
                              try {
                                  Indexer.getInstance().reload();
                                  return null;
                              } catch (IOException e) {
                                  throw new RuntimeException("Loading search index failed", e);
                              } finally {
                                  thread.shutdown();
                              }
                          }
                      }
        );
    }
}
