package com.github.onsdigital.zebedee.persistence.model;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.COLLECTION_OWNER;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.DELETE_MARKER_ADDED;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.DELETE_MARKER_REMOVED;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.DELETE_ROOT;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.DELETE_ROOT_REMOVED;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.PREVIOUS_PUBLISH_DATE;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.PUBLISH_DATE;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.PUBLISH_TYPE;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.create;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests verify the correct {@link CollectionEventMetaData} objects are created for the values provided.
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
        collectionDescription.setName("test");
        collectionDescription.setId(collectionDescription.getName() + "-" + Random.id());
        collectionDescription.setType(CollectionType.scheduled);
        collectionDescription.setPublishDate(date);

        publishTypeScheduledMD = CollectionEventMetaData.create(PUBLISH_TYPE, CollectionType.scheduled.toString());
        publishTypeManualdMD = CollectionEventMetaData.create(PUBLISH_TYPE, CollectionType.manual.toString());
        publishDateMD = CollectionEventMetaData.create(PUBLISH_DATE, date.toString());
    }

    @Test
    public void shouldReturnCollectionCreatedEvent() throws Exception {
        CollectionEventMetaData[] results = CollectionEventMetaData.collectionCreated(collectionDescription);

        assertThat(results.length, equalTo(2));
        assertThat(results[0], equalTo(publishTypeScheduledMD));
        assertThat(results[1], equalTo(publishDateMD));
    }

    @Test
    public void testCollectionCreatedEventOwnerNull() throws Exception {

        CollectionEventMetaData[] results = CollectionEventMetaData.collectionCreated(collectionDescription);

        assertThat(results.length, equalTo(2));
        assertThat(results[0], equalTo(publishTypeScheduledMD));
        assertThat(results[1], equalTo(publishDateMD));
    }

    @Test
    public void testCollectionCreatedEventTypeManual() throws Exception {
        collectionDescription.setType(CollectionType.manual);

        CollectionEventMetaData[] results = CollectionEventMetaData.collectionCreated(collectionDescription);

        assertThat(results.length, equalTo(1));
        assertThat(results[0], equalTo(publishTypeManualdMD));
    }

    @Test
    public void testCollectionCreatedEventDescriptionNull() throws Exception {
        assertThat(CollectionEventMetaData.collectionCreated(null), equalTo(null));
    }

    @Test
    public void testPublishRescheduleWithAllValues() throws Exception {
        Date one = new Date();
        Date two = new Date();

        CollectionEventMetaData previousPubDate = CollectionEventMetaData.create(PREVIOUS_PUBLISH_DATE, one.toString());
        CollectionEventMetaData newPubDate = CollectionEventMetaData.create(PUBLISH_DATE, two.toString());

        CollectionEventMetaData[] results = CollectionEventMetaData.reschedule(one, two);

        assertThat(results.length, equalTo(2));
        assertThat(results[0], equalTo(previousPubDate));
        assertThat(results[1], equalTo(newPubDate));
    }

    @Test
    public void testPublishRescheduleWithNewDateNull() throws Exception {
        CollectionEventMetaData previousPubDate = CollectionEventMetaData.create(PREVIOUS_PUBLISH_DATE, date.toString());

        CollectionEventMetaData[] results = CollectionEventMetaData.reschedule(date, null);

        assertThat(results.length, equalTo(1));
        assertThat(results[0], equalTo(previousPubDate));
    }

    @Test
    public void testPublishRescheduleWithPreviousDateNull() throws Exception {
        CollectionEventMetaData newPubDate = CollectionEventMetaData.create(PUBLISH_DATE, date.toString());

        CollectionEventMetaData[] results = CollectionEventMetaData.reschedule(null, date);

        assertThat(results.length, equalTo(1));
        assertThat(results[0], equalTo(newPubDate));
    }

    @Test
    public void shouldReturnListOfDeleteMarkerAddedValues() throws Exception {
        List<String> uris = new ImmutableList.Builder<String>()
                .add("one", "two", "three").build();

        ImmutableList.Builder<CollectionEventMetaData> expectedListBuilder =
                new ImmutableList.Builder<CollectionEventMetaData>().add(create(DELETE_ROOT, DELETE_ROOT));

        expectedListBuilder.addAll(uris
                .stream().map(uri -> CollectionEventMetaData.create(DELETE_MARKER_ADDED, uri))
                .collect(Collectors.toList()));

        CollectionEventMetaData[] expected = expectedListBuilder.build().toArray(new CollectionEventMetaData[uris.size()]);
        CollectionEventMetaData[] results = CollectionEventMetaData.deleteMarkerAdded(DELETE_ROOT, uris);

        assertThat("Result was null... not what I was expecting.", null == results, is(false));
        assertThat("Expected results size to match that of the input list", results.length, equalTo(uris.size() + 1));
        assertThat("Not as expected.", results, equalTo(expected));
    }

    @Test
    public void shouldReturnListOfDeleteMarkerRemovedValues() throws Exception {
        List<String> uris = new ImmutableList.Builder<String>()
                .add("one", "two", "three").build();

        ImmutableList.Builder<CollectionEventMetaData> expectedListBuilder =
                new ImmutableList.Builder<CollectionEventMetaData>().add(create(DELETE_ROOT_REMOVED, DELETE_ROOT_REMOVED));

        expectedListBuilder.addAll(uris
                .stream().map(uri -> CollectionEventMetaData.create(DELETE_MARKER_REMOVED, uri))
                .collect(Collectors.toList()));

        CollectionEventMetaData[] expected = expectedListBuilder.build().toArray(new CollectionEventMetaData[uris.size()]);
        CollectionEventMetaData[] results = CollectionEventMetaData.deleteMarkerRemoved(DELETE_ROOT_REMOVED, uris);

        assertThat("Result was null... not what I was expecting.", null == results, is(false));
        assertThat("Expected results size to match that of the input list", results.length, equalTo(uris.size() + 1));
        assertThat("Not as expected.", results, equalTo(expected));
    }

}
