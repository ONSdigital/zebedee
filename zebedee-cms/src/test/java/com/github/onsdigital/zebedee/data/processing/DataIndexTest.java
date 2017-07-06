package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;
import com.github.onsdigital.zebedee.data.framework.DataBuilder;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Created by thomasridd on 1/25/16.
 */
public class DataIndexTest extends ZebedeeTestBaseFixture {

    Session publisher;
    Session reviewer;

    ContentReader publishedReader;
    DataBuilder dataBuilder;
    DataPagesGenerator generator;

    DataPagesSet published;

    /**
     * Setup generates an instance of zebedee, a collection, and various DataPagesSet objects (that are test framework generators)
     *
     * @throws Exception
     */
    public void setUp() throws Exception {

        publisher = zebedee.openSession(builder.publisher1Credentials);
        reviewer = zebedee.openSession(builder.reviewer1Credentials);

        dataBuilder = new DataBuilder(zebedee, publisher, reviewer);
        generator = new DataPagesGenerator();

        publishedReader = new FileSystemContentReader(zebedee.getPublished().path);

        // add a set of data to published
        published = generator.generateDataPagesSet("dataprocessor", "published", 2015, 2, "");
        dataBuilder.publishDataPagesSet(published);
    }

    @Test
    public void dataIndex_givenContent_buildsIndex() throws IOException, InterruptedException, BadRequestException {
        // Given
        // content
        ContentReader contentReader = publishedReader;

        // When
        // we build a DataIndex
        DataIndex dataIndex = new DataIndex(contentReader);
        dataIndex.pauseUntilComplete(60);

        // Then
        // indexing should complete with the published timeseries referenced
        assertTrue(dataIndex.cdids().size() > 0);
    }

}