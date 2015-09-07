package com.github.onsdigital.zebedee.json.publishing;

import com.github.onsdigital.zebedee.json.CollectionBase;

import java.util.List;

public class PublishedCollection extends CollectionBase {

    /**
     * A list of {@link com.github.onsdigital.zebedee.json.publishing.Result} for
     * each attempt at publishing this collection.
     */
    public List<Result> publishResults;
}
