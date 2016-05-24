package com.github.onsdigital.zebedee.content.page.visualisation;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by crispin on 16/05/2016.
 */
public class Visualisation extends Page {

    private String uid;
    public String zipTitle;
    private Set<String> filenames;
    private String indexPage = null;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFileUri() {
        return zipTitle;
    }

    public void setFileUri(String fileUri) {
        this.zipTitle = fileUri;
    }

    public Set<String> getFilenames() {
        return filenames;
    }

    public void setFilenames(Set<String> filenames) {
        this.filenames = filenames;

        if (!this.filenames.isEmpty() && this.filenames.size() == 1) {
            this.indexPage = new ArrayList<String>(filenames).get(0);
        }
    }

    public void setZipTitle(String zipTitle) {
        this.zipTitle = zipTitle;
    }

    public String getIndexPage() {
        return indexPage;
    }

    public void setIndexPage(String indexPage) {
        this.indexPage = indexPage;
    }

    @Override
    public PageType getType() {
        return PageType.visualisation;
    }
}
