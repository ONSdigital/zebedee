package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.model.CollectionContentReader;

public class DataPublicationDetails {
    public String datasetUri;
    public String landingPageUri;
    public String fileUri;

    public DatasetLandingPage landingPage;
    public Dataset datasetPage;

    public DataPublicationDetails(CollectionContentReader publishedContentReader, CollectionContentReader reviewedContentReader, String datasetPageUri) {

    }
}
