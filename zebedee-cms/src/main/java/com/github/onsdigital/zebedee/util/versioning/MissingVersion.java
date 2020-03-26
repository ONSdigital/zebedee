package com.github.onsdigital.zebedee.util.versioning;

import com.github.onsdigital.zebedee.content.page.statistics.dataset.Version;

public class MissingVersion {

    private String uri;

    public MissingVersion(Version version) {
        this.uri = version.getUri().toString();
    }

    @Override
    public String toString() {
        return uri;
    }
}
