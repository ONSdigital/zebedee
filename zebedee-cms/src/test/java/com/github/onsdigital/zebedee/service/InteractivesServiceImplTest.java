package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dp.interactives.api.InteractivesAPIClient;
import com.github.onsdigital.dp.interactives.api.exceptions.NoInteractivesInCollectionException;
import com.github.onsdigital.dp.interactives.api.exceptions.ForbiddenException;
import com.github.onsdigital.dp.interactives.api.exceptions.UnauthorizedException;
import com.github.onsdigital.dp.interactives.api.models.Interactive;
import com.github.onsdigital.dp.interactives.api.models.InteractiveMetadata;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.json.CollectionInteractive;
import com.github.onsdigital.zebedee.json.ContentStatus;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class InteractivesServiceImplTest extends TestCase {

    private static final String ID = "12345";
    private static final String COLLECTION_ID = "9876";
    CollectionInteractive interactive = createColInteractive();
    Interactive ix = createInteractive();

    @InjectMocks
    InteractivesServiceImpl service;

    @Mock
    Collection mockCollection;
    @Mock
    CollectionDescription mockCollectionDescription;
    @Mock
    InteractivesAPIClient mockApiClient;

    @Test
    public void testUpdateInteractiveSuccess() throws Exception {

        InteractiveMetadata md = new InteractiveMetadata();
        ix.setURL("madeUpURL");
        ix.setMetadata(md);
        interactive.setId(COLLECTION_ID);

        when(mockCollectionDescription.getInteractive(ID)).thenReturn(Optional.of(interactive));
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
        when(mockCollection.getId()).thenReturn(COLLECTION_ID);
        when(mockApiClient.getInteractive(ID)).thenReturn(ix);

        service.updateInteractiveInCollection(mockCollection, ID, interactive, "user");

        // collection is linked/added and saved
        verify(mockApiClient, times(1)).linkInteractiveToCollection(ID, COLLECTION_ID);
        verify(mockCollectionDescription, times(1)).addInteractive(interactive);
        verify(mockCollection, times(1)).save();
    }

    @Test(expected = InternalServerError.class)
    public void testUpdateInteractiveWhenUnlinkedAndNoUrlThenThrowException() throws Exception {

        InteractiveMetadata md = new InteractiveMetadata();
        md.setCollectionId("");
        ix.setMetadata(md);

        when(mockCollectionDescription.getInteractive(ID)).thenReturn(Optional.of(interactive));
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
        when(mockApiClient.getInteractive(ID)).thenReturn(ix);

        service.updateInteractiveInCollection(mockCollection, ID, interactive, "user");
    }

    @Test(expected = ConflictException.class)
    public void testUpdateInteractiveWhenWrongAssociationThenThrowException() throws Exception {

        when(mockCollectionDescription.getInteractive(ID)).thenReturn(Optional.of(interactive));
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
        when(mockApiClient.getInteractive(ID)).thenReturn(ix);

        service.updateInteractiveInCollection(mockCollection, ID, interactive, "user");
    }

    @Test
    public void testRemoveInteractiveWhenInteractiveNotFoundThenShouldNotInvoke() throws Exception {
        when(mockCollectionDescription.getInteractive(ID)).thenReturn(Optional.empty());
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        service.removeInteractiveFromCollection(mockCollection, ID);

        verify(mockApiClient, never()).deleteInteractive(ID);
        verify(mockCollectionDescription, never()).removeInteractive(interactive);
        verify(mockCollection, never()).save();
    }

    @Test
    public void testRemoveInteractiveNoInteractivesInCollectionException() throws Exception {
        when(mockCollectionDescription.getInteractive(ID)).thenReturn(Optional.of(interactive));
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
        doThrow(new NoInteractivesInCollectionException("Not found")).when(mockApiClient).deleteInteractive(ID);

        service.removeInteractiveFromCollection(mockCollection, ID);

        // If the collection refers to a non existent interactive, remove it from the collection
        verify(mockCollectionDescription).removeInteractive(interactive);
        verify(mockCollection).save();
    }

    @Test
    public void testRemovePublishedInteractiveForbiddenException() throws Exception {
        when(mockCollectionDescription.getInteractive(ID)).thenReturn(Optional.of(interactive));
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
        doThrow(new ForbiddenException("published interactive")).when(mockApiClient).deleteInteractive(ID);

        service.removeInteractiveFromCollection(mockCollection, ID);

        verify(mockCollectionDescription).removeInteractive(interactive);
        verify(mockCollection).save();
    }

    @Test(expected = InternalServerError.class)
    public void testRemoveInteractiveUnauthorizedException() throws Exception {
        when(mockCollectionDescription.getInteractive(ID)).thenReturn(Optional.of(interactive));
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
        doThrow(new UnauthorizedException("Unauthorized")).when(mockApiClient).deleteInteractive(ID);

        service.removeInteractiveFromCollection(mockCollection, ID);

        // Shouldn't remove anything
        verify(mockCollectionDescription, never()).removeInteractive(interactive);
        verify(mockCollection, never()).save();
    }

    @Test
    public void testRemoveInteractive() throws Exception {

        when(mockCollectionDescription.getInteractive(ID)).thenReturn(Optional.of(interactive));
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        service.removeInteractiveFromCollection(mockCollection, ID);

        verify(mockApiClient, times(1)).deleteInteractive(ID);
        verify(mockCollectionDescription).removeInteractive(interactive);
        verify(mockCollection).save();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWhenCollectionIdIsBlankIllegalArgumentIsThrown() throws Exception {

        when(mockCollectionDescription.getId()).thenReturn("");
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        service.publishCollection(mockCollection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWhenCollectionIdIsNullIllegalArgumentIsThrown() throws Exception {

        when(mockCollectionDescription.getId()).thenReturn(null);
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        service.publishCollection(mockCollection);
    }

    @Test
    public void testPublishCollectionWithCorrectCollectionID() {
        String collectionID = "123456789";
        when(mockCollectionDescription.getId()).thenReturn(collectionID);
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        service.publishCollection(mockCollection);

        verify(mockApiClient, times(1)).publishCollection(collectionID);
    }

    private CollectionInteractive createColInteractive() {
        CollectionInteractive ix = new CollectionInteractive();
        ix.setId(ID);
        ix.setState(ContentStatus.Reviewed);
        return ix;
    }

    private Interactive createInteractive() {
        Interactive ix = new Interactive();
        ix.setId(ID);
        InteractiveMetadata md = new InteractiveMetadata();
        md.setTitle("Title");
        md.setCollectionId(COLLECTION_ID);
        ix.setMetadata(md);
        return ix;
    }
}