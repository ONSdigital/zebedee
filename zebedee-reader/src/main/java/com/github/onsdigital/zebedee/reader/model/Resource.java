package com.github.onsdigital.zebedee.reader.model;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;

/**
 * Created by bren on 28/07/15.
 * <p>
 * A resource that accompanies a document ( images, data files, other binary files or textual files )
 */
public class Resource extends Content {
    private InputStreamReader dataReader;
    private String mimeType; //Mime type with no charset information.
    private Charset charset;


    public Resource(URI uri, String name, InputStream dataStream) {
        super(uri, name);
        InputStreamReader inputStreamReader = new InputStreamReader(dataStream);
        this.dataReader = inputStreamReader;
    }

    public InputStreamReader getData() {
        return this.dataReader;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }
}
