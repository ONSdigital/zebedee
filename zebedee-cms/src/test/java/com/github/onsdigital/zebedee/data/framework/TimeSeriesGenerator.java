package com.github.onsdigital.zebedee.data.framework;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by thomasridd on 1/18/16.
 */
public class TimeSeriesGenerator {

    public TimeSeries randomWalk(PageDescription description, boolean withYears, boolean withQuarters, boolean withMonths, int yearsToGenerate, int finalYear) {
        TimeSeries timeSeries = randomWalk(description.getCdid(), withYears, withQuarters, withMonths, yearsToGenerate, finalYear);
        timeSeries.setDescription(description);
        return timeSeries;
    }

    public TimeSeries randomWalk(String name, boolean withYears, boolean withQuarters, boolean withMonths, int yearsToGenerate, int finalYear) {
        TimeSeries timeSeries = new TimeSeries();
        timeSeries.setDescription(new PageDescription());

        timeSeries.getDescription().setCdid(name);
        timeSeries.getDescription().setTitle(name);

        String[] months = "JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC".split(",");
        String[] quarters = "Q1,Q2,Q3,Q4".split(",");

        double val = 100.0;
        NormalDistribution distribution = new NormalDistribution(0, 10);
        distribution.reseedRandomGenerator((long) stringToSeed(name));

        for (int y = finalYear - yearsToGenerate + 1; y <= finalYear; y++) {
            if (withYears) {
                TimeSeriesValue value = new TimeSeriesValue();

                value.date = y + "";
                value.year = y + "";
                value.value = String.format("%.1f", val);
                timeSeries.years.add(value);
            }
            for (int q = 0; q < 4; q++) {
                if (withQuarters) {
                    TimeSeriesValue value = new TimeSeriesValue();
                    value.year = y + "";
                    value.quarter = quarters[q];

                    value.date = y + " " + quarters[q];
                    value.value = String.format("%.1f", val);
                    timeSeries.quarters.add(value);
                }
                for (int mInQ = 0; mInQ < 3; mInQ++) {
                    if (withMonths) {
                        TimeSeriesValue value = new TimeSeriesValue();
                        value.month = months[3 * q + mInQ];
                        value.year = y + "";

                        value.date = y + " " + months[3 * q + mInQ];
                        value.value = String.format("%.1f", val);
                        timeSeries.months.add(value);
                    }
                    val = val + distribution.sample();
                }
            }
        }

        return timeSeries;
    }

    /**
     * Quick method to convert timeseries name to a random seed
     *
     * @param string
     * @return
     */
    private int stringToSeed(String string) {
        return string.chars().sum();
    }
}
