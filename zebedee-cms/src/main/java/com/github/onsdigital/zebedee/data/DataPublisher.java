package com.github.onsdigital.zebedee.data;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateCommand;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.data.processing.DataPublication;
import com.github.onsdigital.zebedee.data.processing.DataPublicationFinder;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class DataPublisher {
    public boolean doNotCompress = false;

    public DataPublisher(boolean doNotCompress) {
        this.doNotCompress = doNotCompress;
    }

    public DataPublisher() {
        this(false);
    }

    /**
     * Run the preprocess routine that processes csdb uploads with the option of skipping timeseries filesaves
     *
     * @param publishedContentReader  reader for the master content
     * @param collectionContentWriter reader for this publications collection content
     * @param saveTimeSeries          the option to skip saving the individual timeseries
     * @param updateCommands
     * @throws IOException
     * @throws ZebedeeException
     * @throws URISyntaxException
     */
    public void preprocessCollection(
            ContentReader publishedContentReader,
            CollectionReader collectionReader,
            ContentWriter collectionContentWriter,
            boolean saveTimeSeries,
            DataIndex dataIndex,
            List<TimeseriesUpdateCommand> updateCommands
    ) throws IOException, ZebedeeException, URISyntaxException {

        // Find all files that need data preprocessing
        List<DataPublication> dataPublications = new DataPublicationFinder().findPublications(publishedContentReader, collectionReader.getReviewed());

        // For each file in this collection
        for (DataPublication dataPublication : dataPublications) {
            // If a file upload exists
            if (dataPublication.hasUpload())
                dataPublication.process(publishedContentReader, collectionReader.getReviewed(), collectionContentWriter, saveTimeSeries, dataIndex, updateCommands);
        }

        applyUpdateCommands(publishedContentReader, collectionReader, collectionContentWriter, dataIndex, updateCommands);
    }

    public void applyUpdateCommands(ContentReader publishedContentReader, CollectionReader collectionReader, ContentWriter collectionContentWriter, DataIndex dataIndex, List<TimeseriesUpdateCommand> updateCommands) throws ZebedeeException, IOException {
        for (TimeseriesUpdateCommand updateCommand : updateCommands) {

            // see if the timeseries is already in the reviewed section
            String timeseriesUri = updateCommand.getDatasetBasedTimeseriesUri(dataIndex);

            if (timeseriesUri != null) {
                try {
                    // if it is then check if it needs updating.
                    Page content = collectionReader.getReviewed().getContent(timeseriesUri);
                    updateTitle(collectionContentWriter, updateCommand, timeseriesUri, content);
                } catch (NotFoundException ex) {
                    // if its not then update the title and write it
                    Page content = publishedContentReader.getContent(timeseriesUri);
                    updateTitle(collectionContentWriter, updateCommand, timeseriesUri, content);
                }
            }
        }
    }

    public void updateTitle(ContentWriter collectionContentWriter, TimeseriesUpdateCommand updateCommand, String uriForCdid, Page content) throws IOException, BadRequestException {
        if (!updateCommand.title.equals(content.getDescription().getTitle())) {
            content.getDescription().setTitle(updateCommand.title);
            collectionContentWriter.writeObject(content, uriForCdid + "/data.json");
        }
    }
}
