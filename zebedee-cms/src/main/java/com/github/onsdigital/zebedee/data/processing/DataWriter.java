package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Version;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.item.ContentItemVersion;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by thomasridd on 1/21/16.
 */
public class DataWriter {
    private ContentWriter contentWriter;
    private ContentReader contentReader;
    private ContentReader pubishedReader;

    public DataWriter(ContentWriter contentWriter, ContentReader contentReader, ContentReader publishedReader) {
        this.contentWriter = contentWriter;
        this.contentReader = contentReader;
        this.pubishedReader = publishedReader;
    }

    public void versionAndSave(DataProcessor processor, DataPublicationDetails details) throws ZebedeeException, IOException {
        // If no change then don't update anything
        int dataChanges = processor.insertions + processor.corrections;

        if (dataChanges > 0) {
            // Version the timeseries
            versionTimeseries(processor.timeSeries, details.getLastDatasetVersion());
        }

        // Save the new page to reviewed
        this.contentWriter.writeObject(processor.timeSeries, processor.timeSeries.getUri().toString() + "/data.json");
    }

    void versionTimeseries(
            TimeSeries timeSeries,
            Version datasetVersion
    ) throws ZebedeeException, IOException {
        String uri = timeSeries.getUri().toString();

        // If no current version of this file exists there is nothing to version
        try {
            TimeSeries current = (TimeSeries) pubishedReader.getContent(uri);
        } catch (ZebedeeException | IOException e) {
            return;
        }

        // create directory in reviewed if it does not exist.
        VersionedContentItem versionedContentItem = new VersionedContentItem(uri);

        // build a version if it doesn't exist
        if (versionedContentItem.versionExists(this.contentReader) == false) {
            ContentItemVersion contentItemVersion = versionedContentItem.createVersion(pubishedReader, this.contentWriter);

            String correctionNotice = datasetVersion != null ? datasetVersion.getCorrectionNotice() : "";
            Date updateDate = datasetVersion != null ? datasetVersion.getUpdateDate() : timeSeries.getDescription().getReleaseDate();

            Version version = new Version();
            version.setUri(URI.create(contentItemVersion.getUri()));
            version.setUpdateDate(updateDate);
            version.setLabel(contentItemVersion.getIdentifier());
            version.setCorrectionNotice(correctionNotice);

            if (timeSeries.getVersions() == null)
                timeSeries.setVersions(new ArrayList<>());

            timeSeries.getVersions().add(version);
        }
    }


}
