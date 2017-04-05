package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.partial.Contact;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateCommand;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.util.URIUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * The workhorse of the data-publisher
 * <p>
 * Processes a single timeseries
 */
public class DataProcessor {
    public int corrections = 0;
    public int insertions = 0;
    public boolean titleUpdated = false;
    public TimeSeries timeSeries = null;

    public DataProcessor() {

    }

    /**
     * Check if a landingPage is listed as a related dataset and if not add it
     *
     * @param page
     * @param landingPageUri
     * @throws URISyntaxException
     */
    private static void checkRelatedDatasets(TimeSeries page, String landingPageUri) throws URISyntaxException {
        List<Link> relatedDatasets = page.getRelatedDatasets();
        if (relatedDatasets == null) {
            relatedDatasets = new ArrayList<>();
        }

        // Check
        boolean datasetNotLinked = true;
        for (Link relatedDataset : relatedDatasets) {
            if (relatedDataset.getUri().toString().equalsIgnoreCase(landingPageUri)) {
                datasetNotLinked = false;
                break;
            }
        }

        // Link if necessary
        if (datasetNotLinked) {
            relatedDatasets.add(new Link(new URI(landingPageUri)));
            page.setRelatedDatasets(relatedDatasets);
        }
    }

    /**
     * @param page
     * @param datasetPage
     */
    private static void addContactDetails(TimeSeries page, DatasetLandingPage datasetPage) {
        if (datasetPage.getDescription().getContact() != null) {
            Contact contact = new Contact();
            if (datasetPage.getDescription().getContact().getName() != null) {
                contact.setName(datasetPage.getDescription().getContact().getName());
            }
            if (datasetPage.getDescription().getContact().getTelephone() != null) {
                contact.setTelephone(datasetPage.getDescription().getContact().getTelephone());
            }
            if (datasetPage.getDescription().getContact().getEmail() != null) {
                contact.setEmail(datasetPage.getDescription().getContact().getEmail());
            }
            if (datasetPage.getDescription().getContact().getOrganisation() != null) {
                contact.setOrganisation(datasetPage.getDescription().getContact().getOrganisation());
            }
            page.getDescription().setContact(contact);
        }
    }

    public TimeSeries processTimeseries(
            ContentReader contentReader,
            DataPublicationDetails details,
            TimeSeries newTimeSeries,
            DataIndex dataIndex
    ) throws ZebedeeException, IOException, URISyntaxException {
        return processTimeseries(contentReader, details, newTimeSeries, dataIndex, Optional.<TimeseriesUpdateCommand>empty());
    }

    /**
     * Take a timeseries as produced by Brian from an upload and combine it with current content
     *
     * @param contentReader
     * @param details
     * @param newTimeSeries @return
     */
    public TimeSeries processTimeseries(
            ContentReader contentReader,
            DataPublicationDetails details,
            TimeSeries newTimeSeries,
            DataIndex dataIndex,
            Optional<TimeseriesUpdateCommand> command
    ) throws ZebedeeException, IOException, URISyntaxException {

        // Get current version of the time series (persists any manually entered data)
        this.timeSeries = initialTimeseries(newTimeSeries, contentReader, details, dataIndex);

        // Add meta from the landing page and timeseries dataset page
        syncLandingPageMetadata(this.timeSeries, details);
        syncTimeSeriesMetadata(this.timeSeries, newTimeSeries);
        applyUpdateCommand(command);

        // Combine the time series values
        DataMerge dataMerge = new DataMerge();
        this.timeSeries = dataMerge.merge(this.timeSeries, newTimeSeries, details.landingPage.getDescription().getDatasetId());

        // Ensure time series labels are up to date
        new TimeSeriesLabeller().applyLabels(this.timeSeries);

        // Log corrections and insertions
        corrections = dataMerge.corrections;
        insertions = dataMerge.insertions;

        return this.timeSeries;
    }

    public void applyUpdateCommand(Optional<TimeseriesUpdateCommand> updateCommand) {
        if (updateCommand.isPresent() && !updateCommand.get().title.equals(this.timeSeries.getDescription().getTitle())) {
            this.timeSeries.getDescription().setTitle(updateCommand.get().title);
            this.titleUpdated = true;
        }
    }

    /**
     * Copy metadata from the landing page
     *
     * @param page
     * @param details
     * @return
     * @throws URISyntaxException
     */
    TimeSeries syncLandingPageMetadata(TimeSeries page, DataPublicationDetails details) throws URISyntaxException {
        PageDescription description = page.getDescription();
        if (description == null) {
            description = new PageDescription();
            page.setDescription(description);
        }
        description.setNextRelease(details.landingPage.getDescription().getNextRelease());
        description.setReleaseDate(details.landingPage.getDescription().getReleaseDate());
        description.setDatasetId(details.landingPage.getDescription().getDatasetId());
        description.setDatasetUri(details.landingPage.getUri());

        // Set some contact details
        addContactDetails(page, details.landingPage);

        // Add the dataset id to sources if necessary
        checkRelatedDatasets(page, details.landingPageUri);

        // Add stats bulletins
        if (details.landingPage.getRelatedDocuments() != null) {
            page.setRelatedDocuments(details.landingPage.getRelatedDocuments());
        }

        return page;
    }

    /**
     * @param inProgress
     * @param newSeries
     * @return
     */
    TimeSeries syncTimeSeriesMetadata(TimeSeries inProgress, TimeSeries newSeries) {
        if (inProgress.getDescription() == null || newSeries.getDescription() == null) {
            logInfo("Error copying metadata in data publisher").log();
        }
        inProgress.getDescription().setCdid(newSeries.getDescription().getCdid());
        inProgress.getDescription().setTitle(newSeries.getDescription().getTitle());
        inProgress.getDescription().setDate(newSeries.getDescription().getDate());
        inProgress.getDescription().setNumber(newSeries.getDescription().getNumber());

        return inProgress;
    }

    /**
     * Get the url for the timeseries when the url has the dataset ID included (new timeseries location)
     *
     * @param series
     * @param details
     * @return
     */
    String getDatasetBasedUriForTimeseries(TimeSeries series, DataPublicationDetails details, DataIndex dataIndex) {
        String cdid = series.getCdid().toLowerCase();
        String datasetId = details.landingPage.getDescription().getDatasetId().toLowerCase();
        String indexed = dataIndex.getUriForCdid(cdid);

        indexed = URIUtils.removeTrailingSlash(indexed);

        if (indexed != null) {
            // if its in the index, just add the dataset id to the end of the existing timeseries URL.
            return String.format("%s/%s", indexed, datasetId);
        } else {
            // if its not in the data index, build the URI based on the dataset location.
            String timeseriesUri = details.getTimeseriesFolder() + "/" + series.getCdid().toLowerCase();
            dataIndex.setUriForCdid(cdid, timeseriesUri);

            String timeseriesDataUri = String.format("%s/%s", timeseriesUri, datasetId);
            return timeseriesDataUri;
        }
    }

    /**
     * Get the starting point for our timeseries by loading a
     *
     * @param series
     * @param contentReader
     * @param details
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    TimeSeries initialTimeseries(TimeSeries series, ContentReader contentReader, DataPublicationDetails details, DataIndex dataIndex) throws ZebedeeException, IOException, URISyntaxException {

        String timeseriesUri = getDatasetBasedUriForTimeseries(series, details, dataIndex);

        try {
            TimeSeries existing = (TimeSeries) contentReader.getContent(timeseriesUri);
            return existing;

        } catch (NotFoundException notFoundAnywhere) {
            // If it doesn't exist create a new empty one using the description
            TimeSeries initial = new TimeSeries();
            initial.setDescription(series.getDescription());
            initial.setUri(new URI(timeseriesUri));

            return initial;
        } catch (IllegalStateException e) {
            logError(e, "Error with timeseries")
                    .addParameter("CDID", series.getCdid())
                    .addParameter("publishUri", timeseriesUri)
                    .log();
            throw e;
        }

    }
}
