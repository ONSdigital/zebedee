package com.github.onsdigital.zebedee;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PermissionsTest {

    Zebedee zebedee;
    Builder builder;

    @Before
    public void setUp() throws Exception {
        builder = new Builder(this.getClass());
        zebedee = new Zebedee(builder.zebedee);
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
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
    public void shouldAddDigitalPublisher() throws IOException {

        // Given
        // A new Digital Publishing user
        String email = "blue@cat.com";

        // When
        // We check view access
        zebedee.permissions.addEditor(email);

        // Then
        // The new user should get both View and Edit permissions:
        assertTrue(zebedee.permissions.canView(email, "/economy"));
        assertTrue(zebedee.permissions.canEdit(email));
    }

    @Test
    public void shouldAddContentOwner() throws IOException {

        // Given
        // A new Content owner user
        String email = "blue@cat.com";

        // When
        // We check view access
        zebedee.permissions.addViewer(email, "/economy");

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

        // When
        // We remove edit access
        zebedee.permissions.removeEditor(email);

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

        // When
        // We remove view access
        zebedee.permissions.removeViewer(email, "/economy");

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
        zebedee.permissions.addViewer(email, "/economy/subpath");

        // When
        // We remove access from the parent path
        zebedee.permissions.removeViewer(email, "/economy");

        // Then
        // The content owner's permissions should be removed from the sub-path
        assertFalse(zebedee.permissions.canView(email, "/economy/subpath"));
        assertFalse(zebedee.permissions.canEdit(email));
    }
}