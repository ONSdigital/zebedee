package com.github.onsdigital.zebedee.json.publishing.request;

public class FileCopy {
    public String source;
    public String target;

    public FileCopy(String sourceUri, String targetUri) {
        this.source = sourceUri;
        this.target = targetUri;
    }
}
