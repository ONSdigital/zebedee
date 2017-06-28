package com.github.onsdigital.zebedee.json;

import com.github.onsdigital.zebedee.user.model.User;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link User}
 */
public class UserTest {



    @Test
    public void shouldAuthenticateUser() throws Exception {

        // Given
        // A user initialised with a password:
        String password = "password";
        User user = new User();
        user.resetPassword(password);

        // When
        // We attempt to authenticate
        boolean result = user.authenticate(password);

        // Then
        // Authentication should succeed
        assertTrue(result);
    }

    @Test
    public void shouldNotAuthenticateNullPassword() throws Exception {

        // Given
        // A user with no password set
        String password = null;
        User user = new User();

        // When
        // We attempt to authenticate
        boolean result = user.authenticate(password);

        // Then
        // We should get an authentication failure, but no error
        assertFalse(result);
    }
}