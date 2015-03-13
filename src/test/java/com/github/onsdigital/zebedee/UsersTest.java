package com.github.onsdigital.zebedee;

import com.github.davidcarboni.cryptolite.Password;
import com.github.onsdigital.zebedee.json.User;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    public void shouldGetUser() throws Exception {

        // Given
        // The users created by Builder
        String email = "patricia@example.com";
        String name = "Patricia Pumpkin";
        String password = "password";

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
}