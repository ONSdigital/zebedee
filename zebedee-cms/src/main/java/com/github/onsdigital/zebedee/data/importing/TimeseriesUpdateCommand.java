package com.github.onsdigital.zebedee.data.importing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A command object to define an update to timeseries data.
 */
public class TimeseriesUpdateCommand {

    public String cdid; // The CDID to define the timeseries to update.

    public String title; // The title to update to.

    public List<String> sourceDatasets;

    public Map<String, Integer> datasetCsvColumn = new HashMap<>(); // the csv column to update for each dataset.
}
