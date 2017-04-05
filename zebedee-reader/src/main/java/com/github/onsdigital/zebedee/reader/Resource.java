package com.github.onsdigital.zebedee.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Created by bren on 30/07/15.
 *
 * Represents a resource file accompanying a json content.json files itself can be read as a resource from file system
 *
 */
public class Resource implements Closeable {
    private String name;
    private String mimeType;
    private InputStream data;
    private URI uri;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InputStream getData() {
        return data;
    }

    public void setData(InputStream data) {
        this.data = data;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public boolean isNotEmpty() throws IOException {
        return getData().available() > 0;
    }

    @Override
    public void close() throws IOException {
        if (data != null) {
            try (InputStream inputStream = data) {
                inputStream.close();
            }
        }
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}
