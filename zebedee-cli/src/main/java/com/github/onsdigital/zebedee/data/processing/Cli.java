package com.github.onsdigital.zebedee.data.processing;

import org.apache.commons.cli.*;

public class Cli {

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption(Option.builder("createcollection")
                .desc("create a new unencrypted collection.")
                .argName("collections directory> <collection name")
                .numberOfArgs(2)
                .build());
        options.addOption(Option.builder("updatetimeseries")
                .desc("update timeseries metadata from the given CSV.")
                .argName("source directory> <destination directory> <csv file")
                .numberOfArgs(3)
                .build());
        options.addOption(Option.builder("removetimeseriesdata")
                .desc("Remove all timeseries data entries for the given resolution ( months | quarters | years )")
                .argName("source directory> <destination directory> <resolution> <CDID...")
                .hasArgs()
                .build());
        options.addOption(Option.builder("removetimeseriesentries")
                .desc("Remove specific timeseries entries for the given CDID")
                .argName("source directory> <destination directory> <CDID> <labels...")
                .hasArgs()
                .build());
        options.addOption(Option.builder("findtimeseriesforsourcedataset")
                .desc("find the timeseries files that exclusively have the given source dataset.")
                .argName("source directory> <source dataset ID")
                .numberOfArgs(2)
                .build());
        options.addOption(Option.builder("movecontent")
                .desc("Move content into a new location in a collection")
                .argName("source directory> <destination directory> <source URI> <destination URI")
                .numberOfArgs(4)
                .build());
        options.addOption(Option.builder("listtimeseries")
                .desc("List time series")
                .argName("source directory> <destination file")
                .numberOfArgs(2)
                .build());

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("createcollection")) {
                CollectionCreator.createCollection(args);
            } else if (line.hasOption("updatetimeseries")) {
                ExistingTimeseriesUpdater.updateTimeseriesData(args);
            } else if (line.hasOption("removetimeseriesdata")) {
                TimeseriesDataRemover.removeTimeseriesData(args);
            } else if (line.hasOption("removetimeseriesentries")) {
                TimeseriesDataRemover.removeTimeseriesEntries(args);
            } else if (line.hasOption("findtimeseriesforsourcedataset")) {
                TimeseriesFinder.findTimeseriesForSourceDataset(args);
            } else if (line.hasOption("movecontent")) {
                ContentMover.moveContent(args);
            } else if (line.hasOption("listtimeseries")) {
                TimeseriesLister.listTimeseries(args);
            } else if (line.hasOption("migratetimeseries")) {
                TimeseriesMigration.migrateTimeseries(args);
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.setWidth(150);
                formatter.printHelp("zebedee-cli", options);
            }

        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            System.exit(1);
        }

        System.exit(0);
    }


}
