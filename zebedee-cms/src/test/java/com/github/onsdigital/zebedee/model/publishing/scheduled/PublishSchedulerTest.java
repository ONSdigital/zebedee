package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import org.elasticsearch.common.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

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
        scheduler = new PublishScheduler(zebedee);
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }

    @Test
    public void scheduledPublish() throws IOException, ZebedeeException {

        // Given a scheduled collection
        CollectionDescription description = new CollectionDescription("collectionName");
        description.type = CollectionType.scheduled;
        description.publishDate = DateTime.now().plusDays(1).toDate();
        Collection collection = Collection.create(description, zebedee, session);

        scheduler.schedulePublish(collection);

        scheduler.cancelPublish(collection);
    }
}
