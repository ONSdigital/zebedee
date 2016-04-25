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
                .desc("Remove timeseries data entries for the given resolution ( months | quarters | years )")
                .argName("source directory> <destination directory> <resolution> <CDID...")
                .hasArgs()
                .build());

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("createcollection")) {
                CollectionCreator.createCollection(args);
            } else if (line.hasOption("updatetimeseries")) {
                ExistingTimeseriesUpdater.updateTimeseriesData(args);
            } else if (line.hasOption("removeTimeseriesData")) {
                TimeseriesDataRemover.removeTimeseriesData(args);
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
