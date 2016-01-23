package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Version;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.*;
import com.github.onsdigital.zebedee.model.content.item.ContentItemVersion;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

/**
 * Created by thomasridd on 1/21/16.
 */
public class DataWriter {
    private CollectionContentWriter collectionContentWriter;
    private CollectionContentReader collectionContentReader;
    private ContentReader pubishedReader;

    public DataWriter(CollectionContentWriter collectionContentWriter, CollectionContentReader collectionContentReader, ContentReader publishedReader) {
        this.collectionContentWriter = collectionContentWriter;
        this.collectionContentReader = collectionContentReader;
        this.pubishedReader = publishedReader;
    }

    public void versionAndSave(DataProcessor processor, DataPublicationDetails details) throws ZebedeeException, IOException {
        // If no change then don't update anything
        if (processor.insertions + processor.corrections == 0) return;

        // Version the timeseries
        versionTimeseries(processor.timeSeries, details.getDatasetCorrectionsNotice());

        // Save the new page to reviewed
        this.collectionContentWriter.writeObject(processor.timeSeries, processor.timeSeries.getUri().toString());

    }

    public void versionTimeseries(
            TimeSeries timeSeries,
            String correctionNotice
    ) throws ZebedeeException, IOException {
        String uri = timeSeries.getUri().toString();

        // If no current version of this file exists there is nothing to version
        try {
            TimeSeries current = (TimeSeries) pubishedReader.getContent(uri);
        } catch (ZebedeeException | IOException e) {
            return;
        }


        // create directory in reviewed if it does not exist.
        VersionedContentItem versionedContentItem = new VersionedContentItem(uri, this.collectionContentWriter);

        // build a version if it doesn't exist
        if (versionedContentItem.versionExists(this.collectionContentReader) == false) {
            ContentItemVersion contentItemVersion = versionedContentItem.createVersion(pubishedReader);

            Version version = new Version();
            version.setUri(URI.create(contentItemVersion.getUri()));
            version.setUpdateDate(timeSeries.getDescription().getReleaseDate());
            version.setLabel(contentItemVersion.getIdentifier());
            version.setCorrectionNotice(correctionNotice);

            if (timeSeries.getVersions() == null)
                timeSeries.setVersions(new ArrayList<>());

            timeSeries.getVersions().add(version);
        }
    }


}
