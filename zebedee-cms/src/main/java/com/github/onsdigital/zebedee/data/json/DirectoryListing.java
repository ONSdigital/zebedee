package com.github.onsdigital.zebedee.data.json;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a directory listing within the site.
 *
 * @author david
 */
public class DirectoryListing {

    private Map<String, String> folders;
    private Map<String, String> files;

    public DirectoryListing() {
        this.files = new HashMap<>();
        this.folders = new HashMap<>();
    }

    public Map<String, String> getFolders() {
        return folders;
    }

    public Map<String, String> getFiles() {
        return files;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        DirectoryListing that = (DirectoryListing) o;

        return new EqualsBuilder()
                .append(folders, that.folders)
                .append(files, that.files)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(folders)
                .append(files)
                .toHashCode();
    }
}