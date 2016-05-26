package com.github.onsdigital.zebedee.model.approval.tasks;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.util.ZipUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ZipFileVerifier {


    public static List<TimeseriesCompressionResult> verifyZipFiles(
            List<TimeseriesCompressionResult> zipFiles,
            ContentReader contentReader,
            ContentReader verificationContentReader,
            ContentWriter verificationContentWriter
    ) {

        List<TimeseriesCompressionResult> failedVerifications = new ArrayList<>();

        for (TimeseriesCompressionResult zipFile : zipFiles) {
            try {
                boolean verified = verifyZipFile(contentReader,
                        verificationContentReader,
                        verificationContentWriter,
                        zipFile);

                if (!verified)
                    failedVerifications.add(zipFile);

                failedVerifications.add(zipFile);
            } catch (IOException | ZebedeeException e) {
                e.printStackTrace();
                failedVerifications.add(zipFile);
            }
        }

        //FileUtils.deleteDirectory(verificationContentReader.getRootFolder().resolve("verification").toFile());
        return failedVerifications;
    }

    public static boolean verifyZipFile(
            ContentReader contentReader,
            ContentReader verificationContentReader,
            ContentWriter verificationContentWriter,
            TimeseriesCompressionResult zipData
    ) throws ZebedeeException, IOException {
        String verificationPath = "verification/" + Random.id();
        InputStream inputStream = contentReader.getResource(zipData.path).getData();
        List<String> unzipped = ZipUtils.unzip(inputStream, verificationPath, verificationContentWriter);

        // count number of files?
        if (unzipped.size() == 0)
            return false;

        if (unzipped.size() != zipData.numberOfFiles) {
            return false;
        }

        // deserialise file and check its a timeseries?
        String verificationPageUri = Paths.get(unzipped.get(0)).getParent().toString();
        TimeSeries page = (TimeSeries) verificationContentReader.getContent(verificationPageUri);

        if (page == null)
            return false;

        return true;
    }
}
