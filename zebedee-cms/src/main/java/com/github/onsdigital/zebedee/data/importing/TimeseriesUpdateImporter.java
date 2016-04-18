package com.github.onsdigital.zebedee.data.importing;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Import data to update timeseries data with.
 */
public interface TimeseriesUpdateImporter {
    /**
     * Import data and return as a collection of timeseries update commands.
     *
     * @return
     */
    ArrayList<TimeseriesUpdateCommand> importData() throws IOException;
}
