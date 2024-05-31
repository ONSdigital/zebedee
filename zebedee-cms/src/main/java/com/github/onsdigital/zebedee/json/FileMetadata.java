package com.github.onsdigital.zebedee.json;

public class FileMetadata {

    public String CollectionID;
    public String FileName;
    public String Path;
    public boolean IsPublishable;
    public String title;
    public int FileSizeBytes;
    public String FileType;
    public String License;
    public String LicenseURL;
    public String getCollectionID() {
        return CollectionID;
    }
    public void setCollectionID(String collectionID) {
        CollectionID = collectionID;
    }
    public String getFileName() {
        return FileName;
    }
    public void setFileName(String fileName) {
        FileName = fileName;
    }
    public String getPath() {
        return Path;
    }
    public void setPath(String path) {
        Path = path;
    }
    public boolean isIsPublishable() {
        return IsPublishable;
    }
    public void setIsPublishable(boolean isPublishable) {
        IsPublishable = isPublishable;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public int getFileSizeBytes() {
        return FileSizeBytes;
    }
    public void setFileSizeBytes(int fileSizeBytes) {
        FileSizeBytes = fileSizeBytes;
    }
    public String getFileType() {
        return FileType;
    }
    public void setFileType(String fileType) {
        FileType = fileType;
    }
    public String getLicense() {
        return License;
    }
    public void setLicense(String license) {
        License = license;
    }
    public String getLicenseURL() {
        return LicenseURL;
    }
    public void setLicenseURL(String licenseURL) {
        LicenseURL = licenseURL;
    }

}
