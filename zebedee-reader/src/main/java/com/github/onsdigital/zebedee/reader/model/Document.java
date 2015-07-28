package com.github.onsdigital.zebedee.reader.model;

import java.net.URI;
import java.nio.charset.Charset;

/**
 * Created by bren on 28/07/15.
 *
 * Represent a document edited, updated and moved around within Zebedee CMS
 *
 * A document can logically have child document, that makes a document a ContentContainer
 */
public class Document extends ContentContainer  {

    private String data;
    private String mimeType; //Mime type with no charset information.
    private Charset charset;

    public Document(URI uri, String name) {
        super(uri, name);
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    /**
     *
     * @return mime type, does not contain charset information, e.g. text/html , use getCharset for chartset
     */
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public boolean isDocument() {
        return true;
    }
}
