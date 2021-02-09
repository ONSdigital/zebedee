package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.json.ApprovalStatus;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.util.slack.Notifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RootTest {

    @Mock
    Notifier notifier;

    @Mock
    Collection notStartedCollection;

    @Mock
    Collection inProgressCollection;

    @Mock
    Collection erroredCollection;

    public static final String EXPECTED_MESSAGE = "Collection approval is in IN_PROGRESS or ERROR state on zebedee startup. It may need to be re-approved manually.";

    @Before
    public void setUp() throws Exception {

        CollectionDescription notStartedDescription = new CollectionDescription();
        notStartedDescription.setApprovalStatus(ApprovalStatus.NOT_STARTED);
        notStartedDescription.setType(CollectionType.scheduled);
        when(notStartedCollection.getDescription()).thenReturn(notStartedDescription);

        CollectionDescription inProgressDescription = new CollectionDescription();
        inProgressDescription.setApprovalStatus(ApprovalStatus.IN_PROGRESS);
        inProgressDescription.setType(CollectionType.scheduled);
        when(inProgressCollection.getDescription()).thenReturn(inProgressDescription);

        CollectionDescription erroredDescription = new CollectionDescription();
        erroredDescription.setApprovalStatus(ApprovalStatus.ERROR);
        erroredDescription.setType(CollectionType.scheduled);
        when(erroredCollection.getDescription()).thenReturn(erroredDescription);
    }

    @Test
    public void testAlertOnInProgressCollections_emptyCollectionsList() {

        // Given an empty list of collections
        Collections.CollectionList collections = new Collections.CollectionList();

        // When the alertOnInProgressCollections function is called
        Root.alertOnInProgressCollections(collections, notifier);

        // Then no notifications are sent
        verify(notifier, never()).collectionAlarm(any(), anyString());
    }

    @Test
    public void testAlertOnInProgressCollections_noInProgressCollections() {

        // Given a list of collections with none in approval state IN_PROGRESS or ERROR
        Collections.CollectionList collections = new Collections.CollectionList();
        collections.add(notStartedCollection);

        // When the alertOnInProgressCollections function is called
        Root.alertOnInProgressCollections(collections, notifier);

        // Then no notifications are sent
        verify(notifier, never()).collectionAlarm(any(), anyString());
    }

    @Test
    public void testAlertOnInProgressCollections_inProgressCollection() {

        // Given a list of collections with one in progress approval state
        Collections.CollectionList collections = new Collections.CollectionList();
        collections.add(notStartedCollection);
        collections.add(inProgressCollection);

        // When the alertOnInProgressCollections function is called
        Root.alertOnInProgressCollections(collections, notifier);

        // Then a notification is sent for the in progress collection
        verify(notifier, times(1)).collectionAlarm(inProgressCollection, EXPECTED_MESSAGE);
    }

    @Test
    public void testAlertOnInProgressCollections_erroredCollection() {

        // Given a list of collections with one in errored approval state
        Collections.CollectionList collections = new Collections.CollectionList();
        collections.add(notStartedCollection);
        collections.add(erroredCollection);

        // When the alertOnInProgressCollections function is called
        Root.alertOnInProgressCollections(collections, notifier);

        // Then a notification is sent for the errored collection
        verify(notifier, times(1)).collectionAlarm(erroredCollection, EXPECTED_MESSAGE);
    }

    @Test
    public void testAlertOnInProgressCollections_erroredAndInProgressCollections() {

        // Given a list of collections with collections in both ERROR and IN_PROGRESS approval states
        Collections.CollectionList collections = new Collections.CollectionList();
        collections.add(inProgressCollection);
        collections.add(notStartedCollection);
        collections.add(erroredCollection);

        // When the alertOnInProgressCollections function is called
        Root.alertOnInProgressCollections(collections, notifier);

        // Then a notification is sent for the errored collection
        verify(notifier, times(1)).collectionAlarm(erroredCollection, EXPECTED_MESSAGE);

        // Then a notification is sent for the in progress collection
        verify(notifier, times(1)).collectionAlarm(inProgressCollection, EXPECTED_MESSAGE);
    }

    @Test
    public void testAlertOnInProgressCollections_multipleCollections() {

        // Given a list of collections with multiple collections in both ERROR and IN_PROGRESS approval states
        Collections.CollectionList collections = new Collections.CollectionList();
        collections.add(inProgressCollection);
        collections.add(inProgressCollection);
        collections.add(notStartedCollection);
        collections.add(notStartedCollection);
        collections.add(erroredCollection);
        collections.add(erroredCollection);

        // When the alertOnInProgressCollections function is called
        Root.alertOnInProgressCollections(collections, notifier);

        // Then notifications is sent for each errored collection
        verify(notifier, times(2)).collectionAlarm(erroredCollection, EXPECTED_MESSAGE);

        // Then notifications are sent for each in progress collection
        verify(notifier, times(2)).collectionAlarm(inProgressCollection, EXPECTED_MESSAGE);
    }
}