package com.github.onsdigital.zebedee.data.importing;

import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.util.URIUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A command object to define an update to timeseries data.
 */
public class TimeseriesUpdateCommand {

    public String cdid; // The CDID to define the timeseries to update.
    public String dataset;
    public String title; // The title to update to.
    public String unit;
    public String preunit;
    public Date releaseDate;
    public String uri;
    public Map<String, Integer> datasetCsvColumn = new HashMap<>(); // the csv column to update for each dataset.

    public TimeseriesUpdateCommand(String cdid, String dataset, String title) {
        this.cdid = cdid;
        this.dataset = dataset;
        this.title = title;
    }

    public TimeseriesUpdateCommand() {
    }

    /**
     * Timeseries Uri now includes the CDID and dataset id. This method resolves the expected URI for an update command.
     *
     * @param dataIndex
     * @return
     */
    public String getDatasetBasedTimeseriesUri(DataIndex dataIndex) {
        String uriForCdid = dataIndex.getUriForCdid(this.cdid);
        uriForCdid = URIUtils.removeTrailingSlash(uriForCdid);
        String datasetBasedTimeseriesUri = String.format("%s/%s", uriForCdid, this.dataset);
        return datasetBasedTimeseriesUri;
    }
}
