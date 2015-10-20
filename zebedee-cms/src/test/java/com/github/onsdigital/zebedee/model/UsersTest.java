package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.json.UserList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class UsersTest {

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
    public void shouldListUsers() throws IOException {
        // Given
        // The users created by Builder
        String email = "patricia@example.com";
        String name = "Patricia Pumpkin";

        // When
        UserList users = zebedee.users.list();

        // Then
        assertNotNull(users);
        assertTrue(users.size() > 0);

        boolean userFound = false;
        for (User user : users) {
            if (user.name.equals(name)
                    && user.email.equals(email)) {
                userFound = true;
            }
        }
        assertTrue(userFound);
    }

    @Test
    public void shouldGetUser() throws Exception {

        // Given
        // The users created by Builder
        String email = "patricia@example.com";
        String name = "Patricia Pumpkin";

        // When
        User user = zebedee.users.get(email);

        // Then
        assertNotNull(user);
        assertEquals(user.name, name);
        assertEquals(user.email, email);
        assertTrue(user.authenticate("password"));
        assertFalse(user.inactive);
    }

    @Test
    public void shouldNotGetNullUser() throws Exception {

        // Given
        // No preconditions

        // When
        User user = null;
        try {
            user = zebedee.users.get(null);
        } catch (BadRequestException e) {
            // Expected - for now
        }

        // Then
        // We should not have an error
        assertNull(user);
    }

    @Test
    public void shouldNotGetUserForNullEmail() throws Exception {

        // Given
        // A null email
        String email = null;

        // When
        User user = null;
        try {
            user = zebedee.users.get(email);
        } catch (BadRequestException e) {
            // Expected - for now
        }

        // Then
        // We should not have an error
        assertNull(user);
    }

    @Test
    public void shouldNotGetUserForBlankEmail() throws Exception {

        // Given
        // A blank email
        String email = "";

        // When
        User user = null;
        try {
            user = zebedee.users.get(email);

        } catch (Exception e) {
            // Ignore
        }

        // Then
        // We should not have an error
        assertNull(user);
    }

    @Test
    public void shouldCreateUser() throws Exception {

        // Given
        // A user set to inactive:
        String email = "mr.rusty@magic.roundabout.com";
        String name = "Mr Rusty";
        User user = new User();
        user.name = "Mr Rusty";
        user.email = "mr.rusty@magic.roundabout.com";

        // When
        User created = zebedee.users.create(user, builder.administrator.email);
        User read = zebedee.users.get(email);

        // Then

        assertNotNull(user);
        assertEquals(created.name, name);
        assertEquals(created.email, email);

        assertNotNull(read);
        assertEquals(read.name, name);
        assertEquals(read.email, email);
    }

    @Test
    public void shouldNotCreateUserPassword() throws Exception {

        // Given
        // A user with a non-blank password:
        String password = "password";
        User user = new User();
        user.name = "Mr Rusty";
        user.email = "mr.rusty@magic.roundabout.com";
        user.resetPassword(password);

        // When
        // We create the user
        User created = zebedee.users.create(user, builder.administrator.email);

        // Then
        // The password should not be set
        assertFalse(created.authenticate(password));
    }

    @Test
    public void shouldCreateUserInactive() throws Exception {

        // Given
        // A user set to inactive:
        User user = new User();
        user.name = "mr.rusty@magic.roundabout.com";
        user.email = "Mr Rusty";
        user.inactive = false;

        // When
        User created = zebedee.users.create(user, builder.administrator.email);

        // Then
        assertTrue(created.inactive);
    }

    @Test
    public void shouldNotCreateNullUser() throws Exception {

        // Given
        // No preconditions

        // When
        User user = zebedee.users.create(null, builder.administrator.email);

        // Then
        // We should not have an error
        assertNull(user);
    }

    @Test
    public void shouldNotCreateUserForNullEmail() throws Exception {

        // Given
        // A null email
        String email = null;
        User user = new User();
        user.email = null;

        // When
        User created  = null;
        //try {
            created = zebedee.users.create(user, builder.administrator.email);
        //} catch (BadRequestException e) {
            // Expected - for now
        //}

        // Then
        // We should not have an error
        assertNull(created);
    }

    @Test
    public void shouldNotCreateUserForBlankEmail() throws Exception {

        // Given
        // A blank email
        String email = "";

        // When
        User user = null;
        try {
            user = zebedee.users.get(null);
        } catch (BadRequestException e) {
            // Expected - for now
        }

        // Then
        // We should not have an error
        assertNull(user);
    }

    @Test
    public void shouldUpdateUser() throws Exception {

        // Given
        // An existing user:
        String email = builder.publisher1.email;
        String name = "Sunnink ewse";
        String lastAdmin = "admin";
        boolean inactive = true;
        User existing = zebedee.users.get(email);

        // When
        // We update the user
        existing.name = name;
        existing.inactive = inactive;
        User updated = zebedee.users.update(existing, lastAdmin);
        User read = zebedee.users.get(email);

        // Then
        // The expected fields should be set:

        assertNotNull(updated);
        assertEquals(name, updated.name);
        assertEquals(inactive, updated.inactive);
        assertEquals(lastAdmin, updated.lastAdmin);

        assertNotNull(read);
        assertEquals(name, read.name);
        assertEquals(inactive, read.inactive);
        assertEquals(lastAdmin, read.lastAdmin);
    }

    @Test
    public void shouldNotUpdateUserPassword() throws Exception {

        // Given
        // An existing user:
        String email = builder.publisher1.email;
        String password = "new password";
        String lastAdmin = builder.administrator.email;
        User existing = zebedee.users.get(email);

        // When
        // We update the user, even though we set the password
        existing.resetPassword(password);
        User updated = zebedee.users.update(existing, lastAdmin);
        User read = zebedee.users.get(email);

        // Then
        // The password should not be included in the update
        assertNotNull(updated);
        assertNotNull(read);
        assertFalse(updated.authenticate(password));
        assertFalse(read.authenticate(password));
        assertEquals(builder.administrator.email, updated.lastAdmin);
        assertEquals(builder.administrator.email, read.lastAdmin);
    }

    @Test
    public void shouldNotUpdateUserEmail() throws Exception {

        // Given
        // An existing user:
        String email = builder.publisher1.email;
        String newEmail = "new@email.com";
        String lastAdmin = builder.administrator.email;;
        User existing = zebedee.users.get(email);

        // When
        existing.email = newEmail;
        User updated = zebedee.users.update(existing, lastAdmin);
        User read = zebedee.users.get(email);
        User readNew = null;
        try {
            readNew = zebedee.users.get(newEmail);
        } catch (Exception e) {
            // Ignore
        }

        // Then

        // It will not have been possible to update
        // because no user exists with this email:
        assertNull(updated);

        // The old user should still exist
        assertNotNull(read);

        // Nothing should have been created with the new email:
        assertNull(readNew);
    }

    @Test
    public void shouldSetPasswordIfAdmin() throws Exception {

        // Given
        // An existing user and an administrator session
        String email = builder.publisher1.email;
        String newPassword = "newPassword";
        Session adminSession = builder.createSession(builder.administrator);

        // When
        // We set the password
        Credentials credentials = new Credentials();
        credentials.email = email;
        credentials.password = newPassword;
        boolean result = zebedee.users.setPassword(adminSession, credentials);

        // Then
        // Authentication should succeed with the new password
        assertTrue(result);
        User user = zebedee.users.get(email);
        assertTrue(user.authenticate(newPassword));
        assertTrue(user.temporaryPassword);
    }

    @Test
    public void shouldSetPasswordIfSelf() throws Exception {

        // Given
        // An existing user and an administrator session
        String email = builder.publisher1.email;
        String newPassword = "newPassword";
        Session selfSession = builder.createSession(builder.publisher1);
        Credentials credentials = new Credentials();
        credentials.email = email;

        // When
        // We set the password
        credentials.password = newPassword;
        credentials.oldPassword = "password";
        boolean result = zebedee.users.setPassword(selfSession, credentials);

        // Then
        // Authentication should succeed with the new password
        assertTrue(result);
        User user = zebedee.users.get(email);
        assertTrue(user.authenticate(newPassword));
        assertFalse(user.temporaryPassword);
    }

    @Test
    public void shouldSetPasswordIfSelfIsAdmin() throws Exception {

        // Given
        // An existing user and an administrator session
        String email = builder.administrator.email;
        String newPassword = "newPassword";
        Session selfSession = builder.createSession(builder.administrator);
        Credentials credentials = new Credentials();
        credentials.email = email;

        // When
        // We set the password
        credentials.password = newPassword;
        credentials.oldPassword = "password";
        boolean result = zebedee.users.setPassword(selfSession, credentials);

        // Then
        // Authentication should succeed with the new password
        assertTrue(result);
        User user = zebedee.users.get(email);
        assertTrue(user.authenticate(newPassword));
        assertFalse(user.temporaryPassword);
    }

    @Test
    public void shouldResetPasswordIfAdmin() throws Exception {

        // Given
        // An existing user and an administrator session
        String email = builder.publisher1.email;
        String newPassword = "newPassword";
        Session adminSession = builder.createSession(builder.administrator);
        Credentials credentials = new Credentials();
        credentials.email = email;

        // When
        // We set the password and update
        credentials.password = newPassword;
        boolean result = zebedee.users.setPassword(adminSession, credentials);

        // Then
        // Authentication should succeed with the new password
        assertTrue(result);
        User user = zebedee.users.get(email);
        assertTrue(user.authenticate(newPassword));
        assertTrue(user.temporaryPassword);
    }

    @Test
    public void  shouldNotSetTemporaryFlagExplicitly() throws Exception {

        // Given
        // An existing user and session
        String email = builder.publisher1.email;
        String newPassword = "newPassword";
        Session selfSession = builder.createSession(builder.publisher1.email);
        Credentials credentials = new Credentials();
        credentials.email = email;
        credentials.password = newPassword;
        credentials.oldPassword = "password";

        // When
        // We set the temporary flag expliticly and update
        credentials.temporaryPassword = true;
        zebedee.users.setPassword(selfSession, credentials);

        // Then
        // The temponary flag should not be set
        User user = zebedee.users.get(email);
        assertFalse(user.temporaryPassword);
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotSetPasswordIfSessionIsNull() throws Exception {

        // Given a null session
        Session session = null;

        // When
        // We attempt to set the password
        Credentials credentials = new Credentials();
        zebedee.users.setPassword(session, credentials);

        // Then an UnauthorizedException is thrown
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldNotSetPasswordIfNotAdmin() throws Exception {

        // Given
        // An existing user and a non-administrator session
        String email = "patricia@example.com";
        String newPassword = Random.password(8);
        Session nonAdminSession = builder.createSession("bernard@example.com");

        // When
        // We attempt to set the password
        Credentials credentials = new Credentials();
        credentials.email = email;
        credentials.password = newPassword;
        boolean result = zebedee.users.setPassword(nonAdminSession, credentials);

        // Then
        // Authentication should not succeed with the new password because it hasn't been changed
        assertFalse(result);
        assertFalse(zebedee.users.get(email).authenticate(newPassword));
    }

    @Test
    public void deleteUser_withAdminAccount_shouldDeleteUser() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {
        // Given
        // An existing user and an admin session (as generated by Builder in setup)
        String email = "patricia@example.com";
        User patricia = zebedee.users.get(email);

        Session adminSession = builder.createSession(builder.administrator);

        // When
        // We delete them
        zebedee.users.delete(adminSession, patricia);

        // Then
        // They should not exist
        UserList users = zebedee.users.list();

        boolean userFound = false;
        for (User user : users) {
            if (user.email.equals(email)) {
                userFound = true;
            }
        }
        assertFalse(userFound);
    }

}