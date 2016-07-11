package com.github.onsdigital.zebedee.data.framework;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.TimeSeriesDataset;
import com.github.onsdigital.zebedee.content.partial.Contact;
import com.github.onsdigital.zebedee.content.partial.Link;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 */
public class DataPagesGenerator {


    public TimeSeries exampleTimeseries(String cdid, String datasetId) {
        return exampleTimeseries(cdid, datasetId, new Date(), true, false, false, 10, 2015);
    }

    /**
     * Build an example timeseries page
     *
     * @param cdid            a cdid for the dataset
     * @param datasetId       the dataset id the timeseries has been generated from
     * @param releaseDate     the release date to set
     * @param withYears       include years
     * @param withQuarters    include quarters
     * @param withMonths      include months
     * @param yearsToGenerate the number of years to generate
     * @param finalYear       the final year to generate
     * @return a random walk timeseries
     */
    public TimeSeries exampleTimeseries(String cdid, String datasetId, Date releaseDate, boolean withYears, boolean withQuarters, boolean withMonths, int yearsToGenerate, int finalYear) {
        TimeSeries timeSeries = new TimeSeries();
        timeSeries.setDescription(new PageDescription());

        timeSeries.getDescription().setCdid(cdid);
        timeSeries.getDescription().setTitle(Random.id());
        timeSeries.getDescription().setReleaseDate(releaseDate);
        timeSeries.getDescription().setContact(dummy());

        String[] months = "JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC".split(",");
        String[] quarters = "Q1,Q2,Q3,Q4".split(",");

        double val = 100.0;
        NormalDistribution distribution = new NormalDistribution(0, 10);
        distribution.reseedRandomGenerator((long) stringToSeed(cdid));

        for (int y = finalYear - yearsToGenerate + 1; y <= finalYear; y++) {
            if (withYears) {
                TimeSeriesValue value = new TimeSeriesValue();

                value.date = y + "";
                value.year = y + "";
                value.value = String.format("%.1f", val);

                value.sourceDataset = datasetId;
                value.updateDate = releaseDate;

                timeSeries.years.add(value);
            }
            for (int q = 0; q < 4; q++) {
                if (withQuarters) {
                    TimeSeriesValue value = new TimeSeriesValue();
                    value.year = y + "";
                    value.quarter = quarters[q];

                    value.date = y + " " + quarters[q];
                    value.value = String.format("%.1f", val);


                    value.sourceDataset = datasetId;
                    value.updateDate = releaseDate;

                    timeSeries.quarters.add(value);
                }
                for (int mInQ = 0; mInQ < 3; mInQ++) {
                    if (withMonths) {
                        TimeSeriesValue value = new TimeSeriesValue();
                        value.month = months[3 * q + mInQ];
                        value.year = y + "";

                        value.date = y + " " + months[3 * q + mInQ];
                        value.value = String.format("%.1f", val);

                        value.sourceDataset = datasetId;
                        value.updateDate = releaseDate;

                        timeSeries.months.add(value);
                    }
                    val = val + distribution.sample();
                }
            }
        }

        return timeSeries;
    }

    /**
     * Build an example timeseries page
     *
     * @param description     standard page metadata (CDID, DatasetId, and ReleaseDate required)
     * @param withYears       include years
     * @param withQuarters    include quarters
     * @param withMonths      include months
     * @param yearsToGenerate the number of years to generate
     * @param finalYear       the final year to generate
     * @return a random walk timeseries
     */
    public TimeSeries exampleTimeseries(PageDescription description, boolean withYears, boolean withQuarters, boolean withMonths, int yearsToGenerate, int finalYear) {
        TimeSeries timeSeries = exampleTimeseries(description.getCdid(), description.getDatasetId(), description.getReleaseDate(), withYears, withQuarters, withMonths, yearsToGenerate, finalYear);
        timeSeries.setDescription(description);
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

    /**
     * Build a boilerplate dataset landing page
     *
     * @param title       the dataset title
     * @param datasetId   the dataset id
     * @param releaseDate a release date
     * @return a data landing page
     */
    public DatasetLandingPage exampleDataLandingPage(String title, String datasetId, Date releaseDate) {
        DatasetLandingPage landingPage = new DatasetLandingPage();
        landingPage.setDescription(new PageDescription());
        landingPage.getDescription().setTitle(title);
        landingPage.getDescription().setDatasetId(datasetId);
        landingPage.getDescription().setReleaseDate(releaseDate);
        landingPage.getDescription().setContact(dummy());
        landingPage.setDatasets(new ArrayList<>());

        return landingPage;
    }


    /**
     * Build a boilerplate time series dataset
     *
     * @param title       the dataset title
     * @param edition     the edition of the dataset (relative to the landing page)
     * @param releaseDate a release date
     * @return a time series dataset
     */
    public TimeSeriesDataset exampleTimeSeriesDataset(String title, String edition, Date releaseDate) {
        TimeSeriesDataset timeSeriesDataset = new TimeSeriesDataset();
        timeSeriesDataset.setDescription(new PageDescription());
        timeSeriesDataset.getDescription().setTitle(title);
        timeSeriesDataset.getDescription().setEdition(edition);
        timeSeriesDataset.getDescription().setReleaseDate(releaseDate);
        timeSeriesDataset.getDescription().setContact(dummy());
        timeSeriesDataset.setDownloads(new ArrayList<>());

        return timeSeriesDataset;
    }

    /**
     * Generate a full set of pages for data publication
     *
     * @param parentUri           the taxonomy node to save at
     * @param datasetId           an id for the dataset
     * @param releaseYear         a release year
     * @param timeSeriesCount     the number of timeseries to generate
     * @param timeSeriesDataCount the number of timeseries data pages to generate
     * @return
     * @throws ParseException
     * @throws URISyntaxException
     */
    public DataPagesSet generateDataPagesSet(
            String parentUri,
            String datasetId,
            int releaseYear,
            int timeSeriesCount,
            String fileName
    ) throws ParseException, URISyntaxException {
        DataPagesSet dataPagesSet = new DataPagesSet();

        String dateAsString = releaseYear + "-01-01 00:00:00.0";
        Date releaseDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").parse(dateAsString);

        DatasetLandingPage landingPage = exampleDataLandingPage("Landing Page " + datasetId, datasetId, releaseDate);

        TimeSeriesDataset timeSeriesDataset = exampleTimeSeriesDataset("Dataset " + datasetId, "current", releaseDate);

        landingPage.setDatasets(new ArrayList<>());
        landingPage.getDatasets().add(new Link(new URI("/" + parentUri + "/datasets/" + datasetId + "/current")));

        DownloadSection downloadSection = new DownloadSection();
        downloadSection.setTitle(datasetId + " time series dataset");
        downloadSection.setCdids(new ArrayList<>());


//        for (int i = 0; i < timeSeriesCount; i++) {
//            TimeSeries timeSeries = exampleTimeseries(datasetId + i, datasetId, releaseDate, true, true, true, 4, releaseYear - 1);
//            downloadSection.getCdids().add(timeSeries.getCdid());
//            timeSeries.setUri(new URI("/" + parentUri + "/timeseries/" + timeSeries.getCdid().toLowerCase()));
//            dataPagesSet.timeSeriesList.add(timeSeries);
//        }

        for (int i = 0; i < timeSeriesCount; i++) {
            TimeSeries timeSeries = exampleTimeseries(datasetId + i, datasetId, releaseDate, true, true, true, 4, releaseYear - 1);
            downloadSection.getCdids().add(timeSeries.getCdid());
            String timeseriesDataUri = String.format("/%s/timeseries/%s/%s", parentUri, timeSeries.getCdid().toLowerCase(), datasetId.toLowerCase());
            timeSeries.setUri(new URI(timeseriesDataUri));
            dataPagesSet.timeSeriesList.add(timeSeries);
        }

        timeSeriesDataset.getDownloads().add(downloadSection);
        timeSeriesDataset.setUri(new URI("/" + parentUri + "/datasets/" + datasetId + "/current"));
        dataPagesSet.timeSeriesDataset = timeSeriesDataset;

        landingPage.setUri(new URI("/" + parentUri + "/datasets/" + datasetId));
        dataPagesSet.datasetLandingPage = landingPage;

        if (fileName.trim().length() == 0) {
            dataPagesSet.fileUri = null;
        } else {
            dataPagesSet.fileUri = "/" + parentUri + "/datasets/" + datasetId + "/" + dataPagesSet.timeSeriesDataset.getDescription().getEdition() + "/" + fileName;
        }
        return dataPagesSet;
    }

    Contact dummy() {
        Contact contact = new Contact();
        contact.setTelephone("0000000");
        contact.setOrganisation("ONS");
        contact.setName("Jukesie");
        contact.setEmail("jukesie@ons.gov.uk");
        return contact;
    }
}
