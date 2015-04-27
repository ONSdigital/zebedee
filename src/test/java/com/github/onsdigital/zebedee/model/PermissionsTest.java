package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PermissionsTest {

    Zebedee zebedee;
    Builder builder;
    Collection inflationCollection;
    Collection labourMarketCollection;

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
    }

    //// Administrator tests ////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void administratorShouldOnlyHaveAdminPermission() throws IOException {

        // Given
        // The Administrator user (NB case-insensitive)
        String email = builder.administrator.email.toUpperCase();

        // When
        // We add the user as an administrator
        boolean adminPermission = zebedee.permissions.isAdministrator(email);
        boolean editPermission = zebedee.permissions.canEdit(email);
        boolean viewPermission = zebedee.permissions.canView(email, labourMarketCollection.description);

        // Then
        // The new user should get only admin permission:
        assertTrue(adminPermission);
        assertFalse(editPermission);
        assertFalse(viewPermission);
    }

    @Test
    public void onlyAdminShouldBeAdministrator() throws IOException {

        // Given
        // A bunch of user email addresses (NB case-insensitive)
        String administratorEmail = builder.administrator.email.toUpperCase();
        String publisherEmail = builder.publisher.email.toUpperCase();
        String viewerEmail = builder.reviewer.email.toUpperCase();
        String unknownEmail = "unknown@example.com";
        String nullEmail = null;

        // When
        // We ask if they are administrators:
        boolean adminIsAdministrator = zebedee.permissions.isAdministrator(administratorEmail);
        boolean publisherIsAdministrator = zebedee.permissions.isAdministrator(publisherEmail);
        boolean viewerIsAdministrator = zebedee.permissions.isAdministrator(viewerEmail);
        boolean unknownIsAdministrator = zebedee.permissions.isAdministrator(unknownEmail);
        boolean nullIsAdministrator = zebedee.permissions.isAdministrator(nullEmail);

        // Then
        // Only the administrator should be, and null should not give an error
        assertTrue(adminIsAdministrator);
        assertFalse(publisherIsAdministrator);
        assertFalse(viewerIsAdministrator);
        assertFalse(unknownIsAdministrator);
        assertFalse(nullIsAdministrator);
    }

    @Test

    public void shouldAddAdministrator() throws IOException {

        // Given
        // A new Administrator user
        String email = "Franklin.D.Roosevelt@whitehouse.gov";
        Session session = zebedee.sessions.create(builder.administrator.email);

        // When
        // We add the user as an administrator (NB case-insensitive)
        zebedee.permissions.addAdministrator(email.toUpperCase(), session);

        // Then
        // The new user should get only admin permission:
        assertTrue(zebedee.permissions.isAdministrator(email));
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddAdministratorIfPublisher() throws IOException {

        // Given
        // A new Administrator user
        String email = "Some.Guy@example.com";
        Session session = zebedee.sessions.create(builder.publisher.email);

        // When
        // We add the user as an administrator (NB case-insensitive)
        zebedee.permissions.addAdministrator(email.toUpperCase(), session);

        // Then
        // We should get unauthorized
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddAdministratorIfViewer() throws IOException {

        // Given
        // A new Administrator user
        String email = "Some.Guy@example.com";
        Session session = zebedee.sessions.create(builder.reviewer.email);

        // When
        // We add the user as an administrator (NB case-insensitive)
        zebedee.permissions.addAdministrator(email.toUpperCase(), session);

        // Then
        // We should get unauthorized
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddAdministratorIfNotLoggedIn() throws IOException {

        // Given
        // A new Administrator user
        String email = "Some.Guy@example.com";
        Session session = null;

        // When
        // We add the user as an administrator (NB case-insensitive)
        zebedee.permissions.addAdministrator(email.toUpperCase(), session);

        // Then
        // We should get unauthorized
    }

    @Test
    public void shouldAddFirstAdministrator() throws IOException {

        // Given
        // The Administrator user has been removed (NB case-insensitive)
        String email = builder.administrator.email;
        Session session = zebedee.sessions.create(builder.administrator.email);
        zebedee.permissions.removeAdministrator(email.toUpperCase(), session);

        // When
        // We add an administrator with any session
        zebedee.permissions.addAdministrator(email.toUpperCase(), null);

        // Then
        // The system should now have an administrator
        assertTrue(zebedee.permissions.hasAdministrator());
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddFirstAdministratorAgain() throws IOException {

        // Given
        // An initial administrator user is present
        String email = "second.administrator@example.com";

        // When
        // We attempt to add a second administrator without having a session
        zebedee.permissions.addAdministrator(email, null);

        // Then
        // We should get unauthorized
    }

    @Test
    public void shouldRemoveAdministrator() throws IOException {

        // Given
        // A short-lived Administrator user (NB case-insensitive)
        String email = "William.Henry.Harrison@whitehouse.gov";
        Session session = zebedee.sessions.create("jukesie@example.com");
        zebedee.permissions.addAdministrator(email.toUpperCase(), session);

        // When
        // We remove the user as an administrator
        zebedee.permissions.removeAdministrator(email, session);

        // Then
        // The new user should get no admin permission
        assertFalse(zebedee.permissions.isAdministrator(email));
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotRemoveAdministratorIfPublisher() throws IOException {

        // Given
        // Users with insufficient permission
        Session publisher = zebedee.sessions.create(builder.publisher.email);

        // When
        // We remove the user as an administrator
        zebedee.permissions.removeAdministrator(builder.administrator.email, publisher);

        // Then
        // The administrator should not have been removed
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotRemoveAdministratorIfViewer() throws IOException {

        // Given
        // Users with insufficient permission
        Session viewer = zebedee.sessions.create(builder.reviewer.email);

        // When
        // We remove the user as an administrator
        zebedee.permissions.removeAdministrator(builder.administrator.email, viewer);

        // Then
        // The administrator should not have been removed
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotRemoveAdministratorIfNotLoggedIn() throws IOException {

        // Given
        // No login session
        Session notLoggedIn = null;

        // When
        // We remove the user as an administrator
        zebedee.permissions.removeAdministrator(builder.administrator.email, notLoggedIn);

        // Then
        // The administrator should not have been removed
    }

    @Test
    public void shouldHaveAnAdministrator() throws IOException {

        // Given
        // An Administrator user

        // When
        // We ask if there is an administrator
        boolean result = zebedee.permissions.hasAdministrator();

        // Then
        // There should be an administrator
        assertTrue(result);
    }

    @Test
    public void shouldHaveNoAdministrator() throws IOException {

        // Given
        // The Administrator user has been removed
        String email = builder.administrator.email;
        Session session = zebedee.sessions.create(builder.administrator.email);
        zebedee.permissions.removeAdministrator(email, session);

        // When
        // We ask if there is an administrator
        boolean result = zebedee.permissions.hasAdministrator();

        // Then
        // There should be no administrator
        assertFalse(result);
    }

    //// Publisher tests ////////////////////////////////////////////////////////////////////////////////////////////


    @Test
    public void publisherShouldHaveEditAndViewPermission() throws IOException {

        // Given
        // The Administrator user (NB case-insensitive)
        String email = builder.publisher.email.toUpperCase();

        // When
        // We add the user as an administrator
        boolean adminPermission = zebedee.permissions.isAdministrator(email);
        boolean editPermission = zebedee.permissions.canEdit(email);
        boolean viewPermission = zebedee.permissions.canView(email, labourMarketCollection.description);

        // Then
        // The new user should get only admin permission:
        assertFalse(adminPermission);
        assertTrue(editPermission);
        assertTrue(viewPermission);
    }

    @Test
    public void onlyPublisherShouldBePublisher() throws IOException {

        // Given
        // A bunch of user email addresses (NB case-insensitive)
        String administratorEmail = builder.administrator.email.toUpperCase();
        String publisherEmail = builder.publisher.email.toUpperCase();
        String viewerEmail = builder.reviewer.email.toUpperCase();
        String unknownEmail = "unknown@example.com";
        String nullEmail = null;

        // When
        // We ask if they are publishers:
        boolean adminIsPublisher = zebedee.permissions.canEdit(administratorEmail);
        boolean publisherIsPublisher = zebedee.permissions.canEdit(publisherEmail);
        boolean viewerIsPublisher = zebedee.permissions.canEdit(viewerEmail);
        boolean unknownIsPublisher = zebedee.permissions.canEdit(unknownEmail);
        boolean nullIsPublisher = zebedee.permissions.canEdit(nullEmail);

        // Then
        // Only the publisher should be, and null should not give an error
        assertFalse(adminIsPublisher);
        assertTrue(publisherIsPublisher);
        assertFalse(viewerIsPublisher);
        assertFalse(unknownIsPublisher);
        assertFalse(nullIsPublisher);
    }

    @Test

    public void shouldAddPublisher() throws IOException {

        // Given
        // A new publisher user
        String email = "Harper.Collins@publishing.team";
        Session session = zebedee.sessions.create(builder.administrator.email);

        // When
        // We add the user as a publisher (NB case-insensitive)
        zebedee.permissions.addEditor(email.toUpperCase(), session);

        // Then
        // The new user should get publish permission:
        assertTrue(zebedee.permissions.canEdit(email));
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddPublisherIfPublisher() throws IOException {

        // Given
        // A new publisher user
        String email = "Some.Guy@example.com";
        Session session = zebedee.sessions.create(builder.publisher.email);

        // When
        // We add the user as a publisher (NB case-insensitive)
        zebedee.permissions.addEditor(email.toUpperCase(), session);

        // Then
        // We should get unauthorized
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddPublisherIfViewer() throws IOException {

        // Given
        // A new publisher user
        String email = "Some.Guy@example.com";
        Session session = zebedee.sessions.create(builder.reviewer.email);

        // When
        // We add the user as a publisher (NB case-insensitive)
        zebedee.permissions.addEditor(email.toUpperCase(), session);

        // Then
        // We should get unauthorized
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotAddPublisherIfNotLoggedIn() throws IOException {

        // Given
        // A new publisher user
        String email = "Some.Guy@example.com";
        Session session = null;

        // When
        // We add the user as a publisher (NB case-insensitive)
        zebedee.permissions.addEditor(email.toUpperCase(), session);

        // Then
        // We should get unauthorized
    }

    @Test
    public void shouldRemovePublisher() throws IOException {

        // Given
        // A short-lived publisher user (NB case-insensitive)
        String email = "Pearson.Longman@whitehouse.gov";
        Session session = zebedee.sessions.create("jukesie@example.com");
        zebedee.permissions.addAdministrator(email.toUpperCase(), session);

        // When
        // We remove the user as a publisher
        zebedee.permissions.removeEditor(email, session);

        // Then
        // The new user should get no publish permission
        assertFalse(zebedee.permissions.canEdit(email));
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotRemovePublisherIfPublisher() throws IOException {

        // Given
        // Users with insufficient permission
        Session publisher = zebedee.sessions.create(builder.publisher.email);

        // When
        // We remove the user as an administrator
        zebedee.permissions.removeEditor(builder.publisher.email, publisher);

        // Then
        // The administrator should not have been removed
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotRemovePublisherIfViewer() throws IOException {

        // Given
        // Users with insufficient permission
        Session viewer = zebedee.sessions.create(builder.reviewer.email);

        // When
        // We remove the user as an administrator
        zebedee.permissions.removeEditor(builder.publisher.email, viewer);

        // Then
        // The administrator should not have been removed
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotRemovePublisherIfNotLoggedIn() throws IOException {

        // Given
        // No login session
        Session notLoggedIn = null;

        // When
        // We remove the user as an administrator
        zebedee.permissions.removeEditor(builder.publisher.email, notLoggedIn);

        // Then
        // The administrator should not have been removed
    }

    ///////////////////////////////////////////////////////////////////////////////////////////

//    @Test
//    public void shouldAddEditor() throws IOException {
//
//        // Given
//        // A new Digital Publishing user
//        String email = "blue@cat.com";
//        Session session = zebedee.sessions.create("jukesie@example.com");
//
//        // When
//        // We check view access
//        zebedee.permissions.addEditor(email, session);
//
//        // Then
//        // The new user should get both View and Edit permissions:
//        assertTrue(zebedee.permissions.canView(email, "/economy"));
//        assertTrue(zebedee.permissions.canEdit(email));
//    }

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

//    @Test
//    public void shouldForbidViewToUnknownUser() throws IOException {
//
//        // Given
//        // An unknown user
//        String email = "blue@cat.com";
//
//        // When
//        // We check view access
//        boolean permission = zebedee.permissions.canView(email, "/economy");
//
//        // Then
//        // Computer should say no
//        assertFalse(permission);
//    }

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

//    @Test
//    public void shouldAllowViewToDigitalPublishing() throws IOException {
//
//        // Given
//        // A user in the Digital Publishing team
//        String email = "patricia@example.com";
//
//        // When
//        // We check view access
//        boolean permission = zebedee.permissions.canView(email, "/economy");
//
//        // Then
//        // Computer should say yes
//        assertTrue(permission);
//    }

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

//    @Test
//    public void shouldAllowViewToContentOwnerForAllowedPath() throws IOException {
//
//        // Given
//        // A Content owner user
//        String email = "ronny@example.com";
//
//        // When
//        // We check view access
//        boolean permission = zebedee.permissions.canView(email, "/economy");
//
//        // Then
//        // Computer should say yes
//        assertTrue(permission);
//    }

//    @Test
//    public void shouldAllowViewToContentOwnerForAllowedSubpath() throws IOException {
//
//        // Given
//        // A Content owner user
//        String email = "ronny@example.com";
//
//        // When
//        // We check view access
//        boolean permission = zebedee.permissions.canView(email, "/economy/subpath");
//
//        // Then
//        // Computer should say yes
//        assertTrue(permission);
//    }

//    @Test
//    public void shouldForbidViewToContentOwnerForUngrantedPath() throws IOException {
//
//        // Given
//        // A Content owner user
//        String email = "ronny@example.com";
//
//        // When
//        // We check view access
//        boolean permission = zebedee.permissions.canView(email, "/labourmarket");
//
//        // Then
//        // Computer should say no
//        assertFalse(permission);
//    }

//    @Test
//    public void shouldAddContentOwner() throws IOException {
//
//        // Given
//        // A new Content owner user
//        String email = "blue@cat.com";
//        Session session = zebedee.sessions.create("jukesie@example.com");
//
//        // When
//        // We check view access
//        ////zebedee.permissions.addViewer(email, "/economy", session);
//
//        // Then
//        // The new user should get only View permission:
//        assertTrue(zebedee.permissions.canView(email, "/economy"));
//        assertFalse(zebedee.permissions.canEdit(email));
//    }

//    @Test
//    public void shouldRemoveDigitalPublisher() throws IOException {
//
//        // Given
//        // A Digital Publishing user
//        String email = "patricia@example.com";
//        Session session = zebedee.sessions.create("jukesie@example.com");
//
//        // When
//        // We remove edit access
//        zebedee.permissions.removeEditor(email, session);
//
//        // Then
//        // The new user should no longer have permission
//        assertFalse(zebedee.permissions.canView(email, "/economy"));
//        assertFalse(zebedee.permissions.canEdit(email));
//    }

//    @Test
//    public void shouldRemoveContentOwner() throws IOException {
//
//        // Given
//        // A Content owner user
//        String email = "ronny@example.com";
//        Session session = zebedee.sessions.create("jukesie@example.com");
//
//        // When
//        // We remove view access
//        zebedee.permissions.removeViewer(email, "/economy", session);
//
//        // Then
//        // The new user should no longer have permission
//        assertFalse(zebedee.permissions.canView(email, "/economy"));
//        assertFalse(zebedee.permissions.canEdit(email));
//    }

//    @Test
//    public void shouldRemoveContentOwnerFromSubPath() throws IOException {
//
//        // Given
//        // A Content owner user is added to a sub-path
//        String email = "ronny@example.com";
//        Session session = zebedee.sessions.create("jukesie@example.com");
//        ////zebedee.permissions.addViewer(email, "/economy/subpath", session);
//
//        // When
//        // We remove access from the parent path
//        zebedee.permissions.removeViewer(email, "/economy", session);
//
//        // Then
//        // The content owner's permissions should be removed from the sub-path
//        assertFalse(zebedee.permissions.canView(email, "/economy/subpath"));
//        assertFalse(zebedee.permissions.canEdit(email));
//    }
}