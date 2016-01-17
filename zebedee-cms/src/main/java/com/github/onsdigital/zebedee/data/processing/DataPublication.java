package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.CollectionContentReader;
import com.github.onsdigital.zebedee.model.CollectionContentWriter;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;

import java.io.IOException;

/**
 * Created by thomasridd on 1/16/16.
 */
public class DataPublication {
    private DataPublicationDetails details = null;
    private TimeSerieses serieses = null;
    private TimeSerieses results = new TimeSerieses();

    /**
     * Get a new Data publication
     *
     * @param collectionReader a CollectionContentReader
     * @param datasetPageUri a
     */
    public DataPublication(CollectionContentReader collectionReader, String datasetPageUri) {

        // Setup the publication by backtracking from the dataset
        details = new DataPublicationDetails(collectionReader, datasetPageUri);
    }

    /**
     * Run the full process operation for this data page
     *
     * @param collectionReader
     * @param collectionWriter
     * @throws IOException
     * @throws ZebedeeException
     */
    public void process(CollectionContentReader collectionReader, CollectionContentWriter collectionWriter) throws IOException, ZebedeeException {

        // send the file for processing
        callDataLink(collectionReader, details.fileUri);

        // Process each returned timeseries
        for(TimeSeries series: serieses) {
            TimeSeriesProcessor processor = new TimeSeriesProcessor(collectionReader, collectionWriter, details);
            results.add(processor.process(series));
        }

        // Generate data files
        DataFileGenerator generator = new DataFileGenerator(collectionWriter);
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
