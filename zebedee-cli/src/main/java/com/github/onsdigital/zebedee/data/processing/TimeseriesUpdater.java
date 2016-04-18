package com.github.onsdigital.zebedee.data.processing;

import java.nio.file.Path;

/**
 * Given as CSV indexed with the timeseries CDID, update each timeseries with the given data.
 */
public class TimeseriesUpdater {


    public static void UpdateTimeseries(Path source, Path destination, Path csvInput) {
        // read the CSV

        // if headers are defined then use them, else just use defaults: CDID,Title

        // read the timeseries file from the root location.

        // update the fields and write the timeseries file to the destination location

    }
}
