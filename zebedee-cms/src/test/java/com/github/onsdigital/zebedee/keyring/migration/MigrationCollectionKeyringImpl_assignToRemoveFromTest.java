package com.github.onsdigital.zebedee.keyring.migration;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.user.model.User;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class MigrationCollectionKeyringImpl_assignToRemoveFromTest extends MigrationCollectionKeyringImplTest {

    @Test
    public void testAssignTo_shouldCallLegacyKeyring() throws Exception {
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        User src = mock(User.class);
        User target = mock(User.class);
        List<CollectionDescription> assignments = mock(List.class);

        keyring.assignTo(src, target, assignments);

        verify(legacyCollectionKeyring, times(1)).assignTo(src, target, assignments);
        verifyZeroInteractions(collectionKeyring);
    }

    @Test
    public void testRemoveFrom_shouldCallLegacyKeyring() throws Exception {
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        User target = mock(User.class);
        List<CollectionDescription> removals = mock(List.class);

        keyring.revokeFrom(target, removals);

        verify(legacyCollectionKeyring, times(1)).revokeFrom(target, removals);
        verifyZeroInteractions(collectionKeyring);
    }
}
