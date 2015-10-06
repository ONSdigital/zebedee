package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Password;
import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.json.UserList;
import org.apache.commons.lang3.StringUtils;
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
        assertTrue(Password.verify("password", user.passwordHash));
        assertFalse(user.inactive);
    }

    @Test
    public void shouldNotGetNullUser() throws Exception {

        // Given
        // No preconditions

        // When
        User user = zebedee.users.get(null);

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
        User user = zebedee.users.get(email);

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
        User user = zebedee.users.get(email);

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
        User created = zebedee.users.create(user);
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
        user.passwordHash = Password.hash(password);

        // When
        User created = zebedee.users.create(user);

        // Then
        assertTrue(StringUtils.isBlank(created.passwordHash));
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
        User created = zebedee.users.create(user);

        // Then
        assertTrue(created.inactive);
    }

    @Test
    public void shouldNotCreateNullUser() throws Exception {

        // Given
        // No preconditions

        // When
        User user = zebedee.users.create(null);

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
        User created = zebedee.users.create(user);

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
        User user = zebedee.users.get(null);

        // Then
        // We should not have an error
        assertNull(user);
    }

    @Test
    public void shouldUpdateUser() throws Exception {

        // Given
        // An existing user:
        String email = "patricia@example.com";
        String name = "Sunnink ewse";
        boolean inactive = true;
        User existing = zebedee.users.get(email);

        // When
        existing.name = name;
        existing.inactive = inactive;
        User updated = zebedee.users.update(existing);
        User read = zebedee.users.get(email);

        // Then

        assertNotNull(updated);
        assertEquals(updated.name, name);
        assertEquals(updated.inactive, inactive);

        assertNotNull(read);
        assertEquals(read.name, name);
        assertEquals(read.inactive, inactive);
    }

    @Test
    public void shouldNotUpdateUserPassword() throws Exception {

        // Given
        // An existing user:
        String email = "patricia@example.com";
        String password = "new password";
        User existing = zebedee.users.get(email);

        // When
        existing.passwordHash = Password.hash(password);
        User updated = zebedee.users.update(existing);
        User read = zebedee.users.get(email);

        // Then

        assertNotNull(updated);
        assertNotEquals(updated.passwordHash, existing.passwordHash);

        assertNotNull(read);
        assertNotEquals(read.passwordHash, existing.passwordHash);
    }

    @Test
    public void shouldNotUpdateUserEmail() throws Exception {

        // Given
        // An existing user:
        String email = "patricia@example.com";
        String newEmail = "patricia@google.com";
        User existing = zebedee.users.get(email);

        // When
        existing.email = newEmail;
        User updated = zebedee.users.update(existing);
        User read = zebedee.users.get(email);
        User readNew = zebedee.users.get(newEmail);

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
    public void shouldAuthenticateUser() throws Exception {

        // Given
        // An existing user:
        String email = "patricia@example.com";
        String password = "password";

        // When
        // We attempt to authenticate
        boolean result = zebedee.users.authenticate(email, password);

        // Then
        // Authentication should succeed
        assertTrue(result);
    }

    @Test
    public void shouldNotAuthenticateBlankEmail() throws Exception {

        // Given
        // A null email address
        String email = null;
        String password = "password";

        // When
        // We attempt to authenticate
        boolean result = zebedee.users.authenticate(email, password);

        // Then
        // We should get an authentication failure, but no error
        assertFalse(result);
    }

    @Test
    public void shouldNotAuthenticateBlankPassword() throws Exception {

        // Given
        // A null email address
        String email = "patricia@example.com";
        String password = null;

        // When
        // We attempt to authenticate
        boolean result = zebedee.users.authenticate(email, password);

        // Then
        // We should get an authentication failure, but no error
        assertFalse(result);
    }

    @Test
    public void shouldSetPassword() throws Exception {

        // Given
        // An existing user and an administrator session
        String email = "patricia@example.com";
        String newPassword = Random.password(8);
        Session adminSession = builder.createSession("jukesie@example.com");

        // When
        // We set the password
        boolean result = zebedee.users.setPassword(email, newPassword, adminSession);

        // Then
        // Authentication should succeed with the new password
        assertTrue(result);
        assertTrue(zebedee.users.authenticate(email, newPassword));
    }

    @Test
    public void shouldNotSetPasswordIfNotAdmin() throws Exception {

        // Given
        // An existing user and a non-administrator session
        String email = "patricia@example.com";
        String newPassword = Random.password(8);
        Session nonAdminSession = builder.createSession("patricia@example.com");

        // When
        // We attempt to set the password
        boolean result = zebedee.users.setPassword(email, newPassword, nonAdminSession);

        // Then
        // Authentication should not succeed with the new password because it hasn't been changed
        assertFalse(result);
        assertFalse(zebedee.users.authenticate(email, newPassword));
    }

    @Test
    public void deleteUser_withAdminAccount_shouldDeleteUser() throws IOException, UnauthorizedException, NotFoundException {
        // Given
        // An existing user and an admin session (as generated by Builder in setup)
        String email = "patricia@example.com";
        User patricia = zebedee.users.get(email);

        Session adminSession = builder.createSession( builder.administrator );

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
//    @Test
//    public void shouldChangePassword() throws Exception {
//
//        // Given
//        // An existing user
//        String email = "patricia@example.com";
//        User existing = zebedee.users.get(email);
//        String password = Random.password(8);
//        String oldHosh = existing.passwordHash;
//
//        // When
//        // We set the new password
//        boolean result = zebedee.users.changePassword(email, oldPassword, password);
//
//        // Then
//        // Authentication should succeed
//        assertTrue(zebedee.users.authenticate(email, password));
//    }
}