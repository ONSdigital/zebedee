package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

public class PublishSchedulerTest {

    private Builder builder;
    private Zebedee zebedee;
    private Session session;
    private PublishScheduler scheduler;

    @Before
    public void setUp() throws Exception {
        builder = new Builder(this.getClass());
        zebedee = new Zebedee(builder.zebedee);
        session = zebedee.openSession(builder.administratorCredentials);
        scheduler = new PublishScheduler();
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }

    // when scheduled, check that the pre-publish task exists and there is no post publish

    // after the pre-publish check the publish task is scheduled.

    @Test
    public void scheduledPublish() throws IOException, ZebedeeException, InterruptedException {

        // Given a scheduled collection
        CollectionDescription description = new CollectionDescription("collectionName");
        description.type = CollectionType.scheduled;
        description.approvedStatus = true;
        description.publishDate = DateTime.now().plusSeconds(2).withMillisOfSecond(0).toDate();
        Collection collection = Collection.create(description, zebedee, session);

        Date startDate = description.publishDate;
        Date prePublishStartDate = new DateTime(description.publishDate).minusSeconds(1).toDate();
        scheduler.schedulePrePublish(collection, zebedee, prePublishStartDate, startDate);

        //Thread.sleep(4000);

        scheduler.cancel(collection);
    }
}
