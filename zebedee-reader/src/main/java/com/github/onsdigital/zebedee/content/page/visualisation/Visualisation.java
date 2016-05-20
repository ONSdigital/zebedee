package com.github.onsdigital.zebedee.content.page.visualisation;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by crispin on 16/05/2016.
 */
public class Visualisation extends Page {

    private String uid;
    public String fileUri;
    private Set<String> filenames;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }

    public Set<String> getFilenames() {
        return filenames;
    }

    public void setFilenames(Set<String> filenames) {
        this.filenames = filenames;
    }

    public void addFilename(String filename) {
        if (this.filenames == null) {
            this.filenames = new HashSet<>();
        }
        if (StringUtils.isNotEmpty(filename)) {
            filenames.add(filename);
        }
    }

    @Override
    public PageType getType() {
        return PageType.visualisation;
    }
}
