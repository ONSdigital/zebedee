package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logDebug;

/**
 * determines the uri's of timeseries dataset download files given the uri of the CSDB file.
 */
public class TimeseriesDatasetFiles {

    private final Path csdbPath;
    private final Path xlsPath;
    private final Path xlsTempPath;
    private final Path csvPath;
    private final Path csvTempPath;
    private final Path datasetPath; // the path of the dataset's data.json
    private final String csdbId;
    private final Path rootPath;

    /**
     * Given a uri of a CSDB file, determine the uri's of the dataset downloads.
     *
     * @param csdbPath
     */
    public TimeseriesDatasetFiles(Path csdbPath) {

        Path root = csdbPath.getParent();
        root = Paths.get(URIUtils.removeLeadingSlash(root.toString())); // remove leading slash from root path.
        String baseName = FilenameUtils.getBaseName(csdbPath.toString());

        this.rootPath = root;
        this.csdbPath = csdbPath;
        this.csvPath = root.resolve(baseName + ".csv");
        this.csvTempPath = root.resolve(baseName + "_updated.csv");
        this.xlsPath = root.resolve(baseName + ".xlsx");
        this.xlsTempPath = root.resolve(baseName + "_updated.xlsx");
        this.csdbId = baseName;
        datasetPath = root.resolve("data.json");
    }

    public static void main(String[] args) {
        TimeseriesDatasetFiles downloads = new TimeseriesDatasetFiles(Paths.get("some/path/ct.csdb"));
        logDebug("Timeseries Dataset Downloads")
                .addParameter("downloads.csdbId", downloads.csdbId)
                .addParameter("downloads.csdbPath", downloads.csdbPath.toString())
                .addParameter("downloads.csvPath", downloads.csvPath.toString())
                .addParameter("downloads.xlsPath", downloads.xlsPath.toString())
                .log();
    }

    public Path getCsdbPath() {
        return csdbPath;
    }

    public Path getXlsPath() {
        return xlsPath;
    }

    public Path getCsvPath() {
        return csvPath;
    }

    public String getCsdbId() {
        return csdbId;
    }

    public Path getXlsTempPath() {
        return xlsTempPath;
    }

    public Path getCsvTempPath() {
        return csvTempPath;
    }

    public Path getDatasetPath() {
        return datasetPath;
    }

    public Path getRootPath() {
        return rootPath;
    }
}
