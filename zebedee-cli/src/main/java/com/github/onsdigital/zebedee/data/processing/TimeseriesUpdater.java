package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.processing.setup.DataIndexBuilder;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.CompoundContentReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logDebug;

/**
 * Created by carlhembrough on 29/09/2016.
 */
public class TimeseriesUpdater {

    public static void updateTimeseries(String[] args) throws InterruptedException, BadRequestException, NotFoundException, IOException {
        // args[1] - source data directory
        // args[2] - destination directory to save the updated timeseries (can be a collection or master)
        // args[3] - A comma seperated list of CDID's to apply the changes to.
        // args[4] - A comma seperated list of dataset ids to apply the changes to.

        Path source = Paths.get(args[1]);
        Path destination = Paths.get(args[2]);

        Set<String> cdids = new TreeSet<>(Arrays.asList(args[3].split(",")));
        Set<String> datasets = new TreeSet<>(Arrays.asList(args[4].split(",")));

        logDebug("CDID's to update").addParameter("cdids", cdids).log();
        logDebug("Datasets to update").addParameter("datasets", datasets).log();

        addTimeseriesNote(source, destination, cdids, datasets);
    }

    private static void addTimeseriesNote(Path source, Path destination, Set<String> cdids, Set<String> datasets) throws InterruptedException, NotFoundException, IOException, BadRequestException {

        ContentReader contentReader = new FileSystemContentReader(source);

        // create a compound reader to check if the file already exists in the destination before reading it from the source.
        CompoundContentReader compoundContentReader = new CompoundContentReader(contentReader);
        compoundContentReader.add(new FileSystemContentReader(destination));

        ContentWriter contentWriter = new ContentWriter(destination);

        DataIndex dataIndex = DataIndexBuilder.buildDataIndex(contentReader);

        for (String cdid : cdids) {
            if (cdid == null || cdid.length() == 0)
                continue;

            String uri = dataIndex.getUriForCdid(cdid.toLowerCase());

            if (uri == null) {
                logDebug("TimeSeries data not found in data index").cdid(cdid).log();
                return;
            }

            for (String dataset : datasets) {

                uri += "/" + dataset;

                TimeSeries timeSeries = (TimeSeries) compoundContentReader.getContent(uri);
                logDebug("Updating timeseries.").cdid(cdid).addParameter("uri", uri).log();

                timeSeries.setNotes(new ArrayList<>());
                timeSeries.getNotes().add("Following a quality review of the Intellectual Property Products (IPP) asset in Gross Fixed Capital Formation (GFCF), analysis has shown that elements in the estimates of purchased software have been double counted from 2001 and this double counting has also uncovered a discrepancy in the modelled data prior to 2001.  Both these issues will be amended for Blue Book 17 and will have an impact on estimates of GFCF and consequently GDP and UK Economic Accounts.  The affected CDIDs are as follows:\\n\\n**IPP:** DLXP, TLPK, EQDT, EQDO\\n\\n**GFCF:** NPQX, NPQS, NPQR, NPQT\\n\\n**Business Investment:** NPEM, NPEK, NPEN, NPEL\\n\\n**GDP:** BKTL, YBHA, ABMI\\n\\n**Sectorised GFCF (In addition to above):** BKVT, DBGP, FCCJ, FCCZ, FDBM, FDCL, FDEH, IHYK, IHY6, IHYM, IHYN, IHYO, IHYP, IHYQ, IHYR, KG60, KG6I, KG6N, KG6R, KG6V, KG6W, KG6Z, KG75, KG76, KG79, KG7M, KG7N, KG7P, KG7Q, KG7S, KG7T, KH98, KH9I, KH9S, NHCJ, NHEG, NQFM, NSSU, RNZD, ROAW, RPYP, RPYQ, RPZW, RQBA, RQBB, RQBR, RQBZ, RQCM\\n\\nFurther detail can be found in [Business Investment: Quarter 2 (Apr to Jun) 2016 revised results][1]. We apologise for any inconvenience this may cause.\\n\\n\\n  [1]: http://www.ons.gov.uk/economy/grossdomesticproductgdp/bulletins/businessinvestment/previousReleases");

                contentWriter.writeObject(timeSeries, uri + "/data.json");
            }
        }

    }
}
