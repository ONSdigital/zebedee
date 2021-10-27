package com.github.onsdigital.zebedee.reader.api.bean;

import com.github.onsdigital.zebedee.search.indexing.Document;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Object representing a response for the PublishedIndex endpoint
 */
public class PublishedIndexResponse {

    private int count;
    private List<Item> items;
    private int limit;
    private int offset;
    @SerializedName("total_count")
    private int totalCount;

    public PublishedIndexResponse() {
        this.items = new ArrayList<>();
    }

    /**
     * Adds a list of documents to the response as Items. Also updates the
     *
     * @param docs  List of Documents to add to response
     */
    public void addDocuments(List<Document> docs) {
        if (docs == null) {
            return;
        }
        docs.stream()
                .map(d -> new Item(d.getUri()))
                .forEach(d -> this.items.add(d));

        count += docs.size();
    }

    public int getCount() {
        return count;
    }

    public List<Item> getItems() {
        return items;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    /**
     * Sub-object representing an individual document returned by the PublishedIndex
     */
    public class Item {
        private String uri;

        public Item(String uri) {
            this.uri = uri;
        }

        public String getUri() {
            return uri;
        }
    }
}
