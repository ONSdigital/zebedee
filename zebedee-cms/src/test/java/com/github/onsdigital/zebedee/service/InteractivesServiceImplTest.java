package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dp.interactives.api.InteractivesAPIClient;
import com.github.onsdigital.dp.interactives.api.models.Interactive;
import com.github.onsdigital.dp.interactives.api.models.InteractiveMetadata;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.json.CollectionInteractive;
import com.github.onsdigital.zebedee.json.ContentStatus;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.InvalidObjectException;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class InteractivesServiceImplTest extends TestCase {

    String id    = "12345";
    String colId = "9876";
    CollectionInteractive interactive = createColInteractive();
    Interactive ix = createInteractive();

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
        interactive.setId(colId);

        when(mockCollectionDescription.getInteractive(id)).thenReturn(Optional.of(interactive));
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
        when(mockCollection.getId()).thenReturn(colId);
        when(mockApiClient.getInteractive(id)).thenReturn(ix);

        InteractivesService service = new InteractivesServiceImpl(mockApiClient);
        service.updateInteractiveInCollection(mockCollection, id, interactive, "user");

        // collection is linked/added and saved
        verify(mockApiClient, times(1)).linkInteractiveToCollection(id, colId);
        verify(mockCollectionDescription, times(1)).addInteractive(interactive);
        verify(mockCollection, times(1)).save();
    }

    @Test(expected = InvalidObjectException.class)
    public void testUpdateInteractiveWhenUnlinkedAndNoUrlThenThrowException() throws Exception {
        
        InteractiveMetadata md = new InteractiveMetadata();
        md.setCollectionId("");
        ix.setMetadata(md);

        when(mockCollectionDescription.getInteractive(id)).thenReturn(Optional.of(interactive));
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
        when(mockApiClient.getInteractive(id)).thenReturn(ix);

        InteractivesService service = new InteractivesServiceImpl(mockApiClient);
        service.updateInteractiveInCollection(mockCollection, id, interactive, "user");
    }

    @Test(expected = ConflictException.class)
    public void testUpdateInteractiveWhenWrongAssociationThenThrowException() throws Exception {
        
        when(mockCollectionDescription.getInteractive(id)).thenReturn(Optional.of(interactive));
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);
        when(mockApiClient.getInteractive(id)).thenReturn(ix);

        InteractivesService service = new InteractivesServiceImpl(mockApiClient);
        service.updateInteractiveInCollection(mockCollection, id, interactive, "user");
    }

    @Test
    public void testRemoveInteractiveWhenInteractiveNotFoundThenShouldNotInvoke() throws Exception {

        when(mockCollectionDescription.getInteractive(id)).thenReturn(Optional.empty());
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        InteractivesService service = new InteractivesServiceImpl(mockApiClient);
        service.removeInteractiveFromCollection(mockCollection, id);

        verify(mockApiClient, times(0)).deleteInteractive(id);
    }

    @Test
    public void testRemoveInteractive() throws Exception {

        when(mockCollectionDescription.getInteractive(id)).thenReturn(Optional.of(interactive));
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        InteractivesService service = new InteractivesServiceImpl(mockApiClient);
        service.removeInteractiveFromCollection(mockCollection, id);

        verify(mockApiClient, times(1)).deleteInteractive(id);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWhenCollectionIdIsBlankIllegalArgumentIsThrown() throws Exception {

        when(mockCollectionDescription.getId()).thenReturn("");
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        InteractivesService service = new InteractivesServiceImpl(mockApiClient);
        service.publishCollection(mockCollection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWhenCollectionIdIsNullIllegalArgumentIsThrown() throws Exception {

        when(mockCollectionDescription.getId()).thenReturn(null);
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        InteractivesService service = new InteractivesServiceImpl(mockApiClient);
        service.publishCollection(mockCollection);
    }

    @Test
    public void testPublishCollectionWithCorrectCollectionID() {
        String collectionID = "123456789";
        when(mockCollectionDescription.getId()).thenReturn(collectionID);
        when(mockCollection.getDescription()).thenReturn(mockCollectionDescription);

        InteractivesService service = new InteractivesServiceImpl(mockApiClient);
        service.publishCollection(mockCollection);

        verify(mockApiClient, times(1)).publishCollection(collectionID);
    }

    private CollectionInteractive createColInteractive()
    {
        CollectionInteractive ix = new CollectionInteractive();
        ix.setId(id);
        ix.setState(ContentStatus.Reviewed);
        return ix;
    }

    private Interactive createInteractive()
    {
        Interactive ix = new Interactive();
        ix.setId(id);
        InteractiveMetadata md = new InteractiveMetadata();
        md.setTitle("Title");
        md.setCollectionId(colId);
        ix.setMetadata(md);
        return ix;
    }
}