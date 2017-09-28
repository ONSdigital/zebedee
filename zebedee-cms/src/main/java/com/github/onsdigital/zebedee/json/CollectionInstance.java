package com.github.onsdigital.zebedee.json;

public class CollectionInstance {

    private String id;
    private String edition;
    private String version;
    private CollectionDataset dataset;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public CollectionDataset getDataset() {
        return dataset;
    }

    public void setDataset(CollectionDataset dataset) {
        this.dataset = dataset;
    }

    public static class CollectionDataset {
        public String id;
        public String title;
        public String href;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CollectionInstance instance = (CollectionInstance) o;

        return getId().equals(instance.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
