package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.CollectionContentReader;
import com.github.onsdigital.zebedee.model.CollectionContentWriter;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.net.URISyntaxException;

public class DataPublication {
    private DataPublicationDetails details = null;
    private TimeSerieses serieses = null;
    private TimeSerieses results = new TimeSerieses();

    /**
     * Setup a a new Data publication
     *  @param publishedContentReader
     * @param reviewedContentReader a CollectionContentReader
     * @param datasetPageUri a
     */
    public DataPublication(ContentReader publishedContentReader, ContentReader reviewedContentReader, String datasetPageUri) throws ZebedeeException, IOException {
        // Setup the publication by backtracking from the dataset
        details = new DataPublicationDetails(publishedContentReader, reviewedContentReader, datasetPageUri);
    }

    public DataPublicationDetails getDetails() {
        return details;
    }

    /**
     * Process a specified collection
     *
     * @param publishedContentReader
     * @param reviewedContentReader
     * @param reviewedContentWriter
     * @throws IOException
     * @throws ZebedeeException
     */
    public void process(ContentReader publishedContentReader, CollectionContentReader reviewedContentReader, CollectionContentWriter reviewedContentWriter) throws IOException, ZebedeeException, URISyntaxException {

        // send the file for processing
        callDataLink(reviewedContentReader, details.fileUri);

        // Process each returned timeseries
        for(TimeSeries series: serieses) {
            // Build new timeseries
            DataProcessor processor = new DataProcessor();
            processor.processTimeseries(publishedContentReader, details, series);

            // Save files
            DataWriter writer = new DataWriter(reviewedContentWriter, reviewedContentReader, publishedContentReader);
            writer.versionAndSave(processor, details);

            // Retain the result to be added to any generated spreadsheet
            results.add(processor.timeSeries);
        }


        // Generate data files
        DataFileGenerator generator = new DataFileGenerator(reviewedContentWriter);
        generator.generateDataFiles(this.details, this.results);
    }



    /**
     * Call Brian with the appropriate file
     *
     * @param collectionReader reader to pick the appropriate file
     * @throws IOException
     * @throws ZebedeeException
     */
    private TimeSerieses callDataLink(CollectionContentReader collectionReader, String fileUri) throws IOException, ZebedeeException {
        TimeSerieses results = null;
        if (fileUri.toLowerCase().endsWith("csdb")) {
            results = new DataLink().callBrianToProcessCSDB(details.fileUri, collectionReader);
        } else {
            results = new DataLink().callBrianToProcessCSV(details.fileUri, collectionReader);
        }
        return results;
    }
}
