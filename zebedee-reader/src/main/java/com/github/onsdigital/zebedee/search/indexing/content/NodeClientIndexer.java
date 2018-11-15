package com.github.onsdigital.zebedee.search.indexing.content;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.search.fastText.FastTextClient;
import com.github.onsdigital.zebedee.search.fastText.FastTextHelper;
import com.github.onsdigital.zebedee.search.indexing.*;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequestBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.elasticSearchLog;

public class NodeClientIndexer extends ZebedeeContentIndexer {

    private final IndexClient indexClient;
    private final FileScanner fileScanner;
    private final ZebedeeReader zebedeeReader;

    public NodeClientIndexer() {
         this.indexClient = IndexClient.getInstance();
         this.fileScanner = new FileScanner();
         this.zebedeeReader = new ZebedeeReader();
    }

    @Override
    public void indexDepartments() {
        // Load the departments
        try {
            List<Department> departmentList = super.loadDepartments();

            // Index
            departmentList
                    .forEach(department -> this.indexClient.createDocument(Index.DEPARTMENTS.getIndex(),
                            DEPARTMENT_TYPE, department.getCode(), department));

        } catch (IOException e) {
            // TODO - better logging
            e.printStackTrace();
        }
    }

    @Override
    public void indexOnsContent(String indexName) {
        long start = System.currentTimeMillis();
        elasticSearchLog("Triggering re-indexing")
                .addParameter("index", indexName)
                .log();
        try {
            this.indexContent(indexName);
            elasticSearchLog("Re-indexing completed")
                    .addParameter("totalTime(ms)", (System.currentTimeMillis() - start))
                    .log();
        } catch (IOException e) {
            throw new IndexingException("Failed re-indexing", e);
        }
    }

    private void indexContent(String indexName) throws IOException {
        List<Page> pages = this.fileScanner.scan().stream()
                .map(document -> {
                    try {
                        return this.zebedeeReader.getPublishedContent(document.getUri());
                    } catch (ZebedeeException | IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (FastTextHelper.Configuration.INDEX_EMBEDDING_VECTORS) {
            // Set embedding vectors
            try {
                pages = FastTextClient.getInstance().generateEmbeddingVectors(pages, 1000);
            } catch (Exception e) {
                elasticSearchLog("Error generating embedding vectors").log();
                throw new IndexingException("Failed re-indexing", e);
            }
        }

        // Index the pages
        try (BulkProcessor bulkProcessor = this.indexClient.getBulkProcessor()) {
            pages.stream()
                    .map(Page::toSearchDocument)
                    .forEach(page -> {
                        IndexRequestBuilder requestBuilder = this.indexClient.createIndexRequest(
                                indexName,
                                page.getType().name(),
                                page.getUri().toString(),
                                page
                        );
                        bulkProcessor.add(requestBuilder.request());
                    });
        }
    }
}
