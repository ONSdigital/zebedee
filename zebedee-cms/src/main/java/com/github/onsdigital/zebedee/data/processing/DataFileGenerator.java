package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.model.CollectionContentReader;
import com.github.onsdigital.zebedee.model.CollectionContentWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;

/**
 * Created by thomasridd on 1/16/16.
 */
public class DataFileGenerator {

    String baseUri = "";
    String baseFilename = "";
    CollectionContentWriter collectionWriter;

    public DataFileGenerator(CollectionContentWriter collectionWriter) {
        this.collectionWriter = collectionWriter;
    }

    public void generateDataFiles(DataPublicationDetails details, TimeSerieses serieses) {
        preprocess();
        writeCSV(this.collectionWriter, baseUri + "/" + baseFilename + ".csv");
        writeXLS(this.collectionWriter, baseUri + "/" + baseFilename + ".xlsx");
        writeJSONStat(this.collectionWriter, baseUri + "/" + baseFilename + ".json");
    }

    private void preprocess() {

    }
    private void writeXLS(CollectionContentWriter collectionContentWriter, String uri) {

    }
    private void writeCSV(CollectionContentWriter collectionContentWriter, String uri) {

    }
    private void writeJSONStat(CollectionContentWriter collectionContentWriter, String uri) {

    }

}
