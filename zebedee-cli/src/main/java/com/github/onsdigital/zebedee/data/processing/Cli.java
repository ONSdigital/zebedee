package com.github.onsdigital.zebedee.data.processing;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Cli {

    public static void main(String[] args) {

        if (args == null || args.length < 1) {
            System.out.println("Please provide the name of the command you wish to run.");
            return;
        }

        String command = args[0];

        if (command.equalsIgnoreCase("updateTimeseries")) {
            updateTimeseriesData(args);
        }
    }

    private static void updateTimeseriesData(String[] args) {

        // args[1] - source data directory
        // args[2] - destination directory to save the updated timeseries (can be a collection or master)
        // args[3] - path to the CSV file containing the names

        Path source = Paths.get(args[1]);
        Path destination = Paths.get(args[2]);
        Path csvInput = Paths.get(args[3]);

        TimeseriesUpdater.UpdateTimeseries(source, destination, csvInput);
    }
}
