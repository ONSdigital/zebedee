package com.github.onsdigital.zebedee.data.importing;

/**
 * A command object to define an update to timeseries data.
 */
public class TimeseriesUpdateCommand {

    public String cdid; // The CDID to define the timeseries to update.

    public String title; // The title to update to.

}
