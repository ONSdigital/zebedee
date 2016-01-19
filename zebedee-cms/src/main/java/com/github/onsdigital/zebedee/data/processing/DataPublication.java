package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.CollectionContentReader;
import com.github.onsdigital.zebedee.model.CollectionContentWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;

public class DataPublication {
    private DataPublicationDetails details = null;
    private TimeSerieses serieses = null;
    private TimeSerieses results = new TimeSerieses();

    /**
     * Get a new Data publication
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
    public void process(CollectionContentReader publishedContentReader, CollectionContentReader reviewedContentReader, CollectionContentWriter reviewedContentWriter) throws IOException, ZebedeeException {

        // send the file for processing
        callDataLink(reviewedContentReader, details.fileUri);

        // Process each returned timeseries
        for(TimeSeries series: serieses) {
            results.add(new DataProcessor().processTimeseries(publishedContentReader, reviewedContentReader, reviewedContentWriter, details, series));
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
    private void callDataLink(CollectionContentReader collectionReader, String fileUri) throws IOException, ZebedeeException {
        if (fileUri.toLowerCase().endsWith("csdb")) {
            serieses = new DataLink().callBrianToProcessCSDB(details.fileUri, collectionReader);
        } else {
            serieses = new DataLink().callBrianToProcessCSV(details.fileUri, collectionReader);
        }
    }
}
