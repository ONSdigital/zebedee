package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.debugMessage;

/**
 * determines the uri's of timeseries dataset download files given the uri of the CSDB file.
 */
public class TimeseriesDatasetDownloads {

    private final Path csdbPath;
    private final Path xlsPath;
    private final Path xlsTempPath;
    private final Path csvPath;
    private final Path csvTempPath;
    private final String csdbId;

    /**
     * Given a uri of a CSDB file, determine the uri's of the dataset downloads.
     *
     * @param csdbPath
     */
    public TimeseriesDatasetDownloads(Path csdbPath) {

        Path root = csdbPath.getParent();
        root = Paths.get(URIUtils.removeLeadingSlash(root.toString())); // remove leading slash from root path.
        String baseName = FilenameUtils.getBaseName(csdbPath.toString());

        this.csdbPath = csdbPath;
        this.csvPath = root.resolve(baseName + ".csv");
        this.csvTempPath = root.resolve(baseName + "_updated.csv");
        this.xlsPath = root.resolve(baseName + ".xlsx");
        this.xlsTempPath = root.resolve(baseName + "_updated.xlsx");
        this.csdbId = baseName;
    }

    public static void main(String[] args) {
        TimeseriesDatasetDownloads downloads = new TimeseriesDatasetDownloads(Paths.get("some/path/ct.csdb"));
        debugMessage("Timeseries Dataset Downloads")
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
}
