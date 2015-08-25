package com.github.onsdigital.zebedee.reader.api.bean;

/**
 * Created by bren on 19/08/15.
 * <p>
 * Represents file size in bytes
 */
public class FileSize {

    private Long fileSize;

    public FileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
