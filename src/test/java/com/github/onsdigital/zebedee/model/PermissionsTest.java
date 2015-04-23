package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.json.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PermissionsTest {

    Zebedee zebedee;
    Builder builder;
    Collection inflationCollection;
    Collection labourMarketCollection;

    List<Builder> cleanup = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        builder = new Builder(this.getClass());
        zebedee = new Zebedee(builder.zebedee);
        inflationCollection = new Collection(builder.collections.get(0), zebedee);
        labourMarketCollection = new Collection(builder.collections.get(1), zebedee);
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
        for (Builder additional : cleanup) {
            additional.delete();
        }
        cleanup.clear();
    }

    @Test
    public void shouldBeAdministrator() throws IOException {

        // Given
        // A new Administrator user
        String email = "blue@cat.com";
        Session session = zebedee.sessions.create("jukesie@example.com");

        // When
        // We add the user as an administrator
        zebedee.permissions.addAdministrator(email, session);

        // Then
        // The new user should get only admin permission:
        assertTrue(zebedee.permissions.isAdministrator(email));
        assertFalse(zebedee.permissions.canView(email, inflationCollection.description));
        assertFalse(zebedee.permissions.canView(email, labourMarketCollection.description));
        assertFalse(zebedee.permissions.canEdit(email));
    }

    @Test
    public void shouldAddAdministrator() throws IOException {

        // Given
        // A new Administrator user
        String email = "blue@cat.com";
        Session session = zebedee.sessions.create("jukesie@example.com");

        // When
        // We add the user as an administrator
        zebedee.permissions.addAdministrator(email, session);

        // Then
        // The new user should get only admin permission:
        assertTrue(zebedee.permissions.isAdministrator(email));
        assertFalse(zebedee.permissions.canView(email, inflationCollection.description));
        assertFalse(zebedee.permissions.canView(email, labourMarketCollection.description));
        assertFalse(zebedee.permissions.canEdit(email));
    }

    @Test
    public void shouldRemoveAdministrator() throws IOException {

        // Given
        // An Administrator user
        String email = "William.Henry.Harrison@whitehouse.gov";
        Session session = zebedee.sessions.create("jukesie@example.com");
        zebedee.permissions.addAdministrator(email, session);

        // When
        // We remove the user as an administrator
        zebedee.permissions.removeAdministrator(email, session);

        // Then
        // The new user should get only admin permission:
        assertFalse(zebedee.permissions.isAdministrator(email));
        assertFalse(zebedee.permissions.canView(email, inflationCollection.description));
        assertFalse(zebedee.permissions.canView(email, labourMarketCollection.description));
        assertFalse(zebedee.permissions.canEdit(email));
    }

    @Test
    public void shouldAddEditor() throws IOException {

        // Given
        // A new Digital Publishing user
        String email = "blue@cat.com";
        Session session = zebedee.sessions.create("jukesie@example.com");

        // When
        // We check view access
        zebedee.permissions.addEditor(email, session);

        // Then
        // The new user should get both View and Edit permissions:
        assertTrue(zebedee.permissions.canView(email, "/economy"));
        assertTrue(zebedee.permissions.canEdit(email));
    }

    @Test
    public void shouldForbidEditToUnknownUser() throws IOException {

        // Given
        // An unknown user
        String email = "blue@cat.com";

        // When
        // We check edit access
        boolean permission = zebedee.permissions.canEdit(email);

        // Then
        // Computer should say no
        assertFalse(permission);
    }

    @Test
    public void shouldForbidViewToUnknownUser() throws IOException {

        // Given
        // An unknown user
        String email = "blue@cat.com";

        // When
        // We check view access
        boolean permission = zebedee.permissions.canView(email, "/economy");

        // Then
        // Computer should say no
        assertFalse(permission);
    }

    @Test
    public void shouldAllowEditToDigitalPublishing() throws IOException {

        // Given
        // A user in the Digital Publishing team
        String email = "patricia@example.com";

        // When
        // We check edit access
        boolean permission = zebedee.permissions.canEdit(email);

        // Then
        // Computer should say yes
        assertTrue(permission);
    }

    @Test
    public void shouldAllowViewToDigitalPublishing() throws IOException {

        // Given
        // A user in the Digital Publishing team
        String email = "patricia@example.com";

        // When
        // We check view access
        boolean permission = zebedee.permissions.canView(email, "/economy");

        // Then
        // Computer should say yes
        assertTrue(permission);
    }

    @Test
    public void shouldForbidEditToContentOwner() throws IOException {

        // Given
        // A Content owner user
        String email = "ronny@example.com";

        // When
        // We check edit access
        boolean permission = zebedee.permissions.canEdit(email);

        // Then
        // Computer should say no
        assertFalse(permission);
    }

    @Test
    public void shouldAllowViewToContentOwnerForAllowedPath() throws IOException {

        // Given
        // A Content owner user
        String email = "ronny@example.com";

        // When
        // We check view access
        boolean permission = zebedee.permissions.canView(email, "/economy");

        // Then
        // Computer should say yes
        assertTrue(permission);
    }

    @Test
    public void shouldAllowViewToContentOwnerForAllowedSubpath() throws IOException {

        // Given
        // A Content owner user
        String email = "ronny@example.com";

        // When
        // We check view access
        boolean permission = zebedee.permissions.canView(email, "/economy/subpath");

        // Then
        // Computer should say yes
        assertTrue(permission);
    }

    @Test
    public void shouldForbidViewToContentOwnerForUngrantedPath() throws IOException {

        // Given
        // A Content owner user
        String email = "ronny@example.com";

        // When
        // We check view access
        boolean permission = zebedee.permissions.canView(email, "/labourmarket");

        // Then
        // Computer should say no
        assertFalse(permission);
    }

    @Test
    public void shouldAddContentOwner() throws IOException {

        // Given
        // A new Content owner user
        String email = "blue@cat.com";
        Session session = zebedee.sessions.create("jukesie@example.com");

        // When
        // We check view access
        ////zebedee.permissions.addViewer(email, "/economy", session);

        // Then
        // The new user should get only View permission:
        assertTrue(zebedee.permissions.canView(email, "/economy"));
        assertFalse(zebedee.permissions.canEdit(email));
    }

    @Test
    public void shouldRemoveDigitalPublisher() throws IOException {

        // Given
        // A Digital Publishing user
        String email = "patricia@example.com";
        Session session = zebedee.sessions.create("jukesie@example.com");

        // When
        // We remove edit access
        zebedee.permissions.removeEditor(email, session);

        // Then
        // The new user should no longer have permission
        assertFalse(zebedee.permissions.canView(email, "/economy"));
        assertFalse(zebedee.permissions.canEdit(email));
    }

    @Test
    public void shouldRemoveContentOwner() throws IOException {

        // Given
        // A Content owner user
        String email = "ronny@example.com";
        Session session = zebedee.sessions.create("jukesie@example.com");

        // When
        // We remove view access
        zebedee.permissions.removeViewer(email, "/economy", session);

        // Then
        // The new user should no longer have permission
        assertFalse(zebedee.permissions.canView(email, "/economy"));
        assertFalse(zebedee.permissions.canEdit(email));
    }

    @Test
    public void shouldRemoveContentOwnerFromSubPath() throws IOException {

        // Given
        // A Content owner user is added to a sub-path
        String email = "ronny@example.com";
        Session session = zebedee.sessions.create("jukesie@example.com");
        ////zebedee.permissions.addViewer(email, "/economy/subpath", session);

        // When
        // We remove access from the parent path
        zebedee.permissions.removeViewer(email, "/economy", session);

        // Then
        // The content owner's permissions should be removed from the sub-path
        assertFalse(zebedee.permissions.canView(email, "/economy/subpath"));
        assertFalse(zebedee.permissions.canEdit(email));
    }
}