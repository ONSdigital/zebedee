package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.model.CollectionContentReader;
import com.github.onsdigital.zebedee.reader.CollectionReader;

public class DataPublicationDetails {
    public String datasetUri;
    public String landingPageUri;
    public String fileUri;

    public DatasetLandingPage landingPage;
    public Dataset datasetPage;

    public DataPublicationDetails(CollectionContentReader reader, String datasetPageUri) {

    }

    public String fileType() {
        if (fileUri.toLowerCase().endsWith("csdb")) {
            return "csdb";
        } else {
            return "csv";
        }
    }
}
