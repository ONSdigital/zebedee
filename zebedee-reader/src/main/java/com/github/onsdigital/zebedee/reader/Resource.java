package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.util.VariableUtils;

import java.io.InputStream;

/**
 * Created by bren on 30/07/15.
 *
 * Represents a resource file accompanying a json content.json files itself can be read as a resource from file system
 *
 */
public class Resource {
    private String name;
    private String mimeType;
    private String encoding = VariableUtils.getSystemProperty("file.encoding"); //default resource encoding is system encoding
    private InputStream data;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
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
}
