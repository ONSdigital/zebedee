package com.github.onsdigital.zebedee.persistence.model;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.CollectionOwner;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.COLLECTION_OWNER;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.PUBLISH_DATE;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.PUBLISH_TYPE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by dave on 6/8/16.
 */
public class CollectionEventMetaDataTest {

    private CollectionEventMetaData publishTypeScheduledMD;
    private CollectionEventMetaData publishTypeManualdMD;
    private CollectionEventMetaData publishDateMD;
    private CollectionEventMetaData collectionOwnerMD;
    private Date date;
    private CollectionDescription collectionDescription;

    @Before
    public void setUp() throws Exception {
        date = new Date();

        collectionDescription = new CollectionDescription();
        collectionDescription.name = "test";
        collectionDescription.id = collectionDescription.name + "-" + Random.id();
        collectionDescription.type = CollectionType.scheduled;
        collectionDescription.publishDate = date;
        collectionDescription.collectionOwner = CollectionOwner.PUBLISHING_SUPPORT;

        publishTypeScheduledMD = CollectionEventMetaData.create(PUBLISH_TYPE, CollectionType.scheduled.toString());
        publishTypeManualdMD = CollectionEventMetaData.create(PUBLISH_TYPE, CollectionType.manual.toString());
        publishDateMD = CollectionEventMetaData.create(PUBLISH_DATE, date.toString());
        collectionOwnerMD = CollectionEventMetaData.create(COLLECTION_OWNER, CollectionOwner.PUBLISHING_SUPPORT.name());
    }

    @Test
    public void shouldReturnCollectionCreatedEvent() throws Exception {
        CollectionEventMetaData[] results = CollectionEventMetaData.collectionCreated(collectionDescription);

        assertThat(results.length, equalTo(3));
        assertThat(results[0], equalTo(publishTypeScheduledMD));
        assertThat(results[1], equalTo(publishDateMD));
        assertThat(results[2], equalTo(collectionOwnerMD));
    }

    @Test
    public void testCollectionCreatedEventOwnerNull() throws Exception {
        collectionDescription.collectionOwner = null;

        CollectionEventMetaData[] results = CollectionEventMetaData.collectionCreated(collectionDescription);

        assertThat(results.length, equalTo(2));
        assertThat(results[0], equalTo(publishTypeScheduledMD));
        assertThat(results[1], equalTo(publishDateMD));
    }

    @Test
    public void testCollectionCreatedEventTypeManual() throws Exception {
        collectionDescription.type = CollectionType.manual;

        CollectionEventMetaData[] results = CollectionEventMetaData.collectionCreated(collectionDescription);

        assertThat(results.length, equalTo(2));
        assertThat(results[0], equalTo(publishTypeManualdMD));
        assertThat(results[1], equalTo(collectionOwnerMD));
    }

    @Test
    public void testCollectionCreatedEventDescriptionNull() throws Exception {
        assertThat(CollectionEventMetaData.collectionCreated(null), equalTo(null));
    }

}
