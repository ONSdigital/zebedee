package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link KeyringCache}.
 */
public class KeyringCacheCacheTest extends ZebedeeTestBaseFixture {

    private KeyringCache keyringCache;
    private Session session;
    private User user;

    @Override
    public void setUp() throws Exception {
        keyringCache = zebedee.getKeyringCache();

        user = user();

        session = new Session();
        session.setEmail(builder.publisher1.getEmail());
        session.setId("1234");
        session.setLastAccess(new Date());
        session.setStart(new Date());
    }

    @Test
    public void shouldPutAndGetKeyring() throws Exception {

        // Given
        // A user with a session

        // When
        // We put the user's keyring
        keyringCache.put(user, session);
        Keyring keyring = zebedee.getKeyringCache().get(user);

        // Then
        // We should be able to get the user's keyring
        assertNotNull(keyring);
        assertTrue(keyring.isUnlocked());
    }

    @Test
    public void shouldNotPutNulls() throws Exception {

        // Given
        // A null user and missing session
        User nullUser = null;
        User noSessionUser = user();
        session = null;

        // When
        // We put the user's keyring
        keyringCache.put(nullUser, session);
        keyringCache.put(noSessionUser, session);

        // Then
        // We should get no error and nothing should be present in the cache
        assertNull(keyringCache.get(nullUser));
        assertNull(keyringCache.get(noSessionUser));
    }

    @Test
    public void shouldGetNullsWithoutError() throws Exception {

        // Given
        // A null user and missing session
        User nullUser = null;
        User noSessionUser = user();

        // When
        // We put the user's keyring
        Keyring nullUserKeyring = keyringCache.get(nullUser);
        Keyring nullSessioKeyring = keyringCache.get(noSessionUser);

        // Then
        // We should get no error and nothing should be returned from the cache
        assertNull(nullUserKeyring);
        assertNull(nullSessioKeyring);
    }

    @Test
    public void shouldRemoveKeyring() throws Exception {

        // Given
        // A keyring in the cache
        User user = user();

        keyringCache.put(user, session);

        // When
        // We remove the user's keyring
        zebedee.getKeyringCache().remove(session);

        // Then
        // The user's keyring should not be present in the cache
        assertNull(zebedee.getKeyringCache().get(user));
    }

    @Test
    public void shouldRemoveNullWithoutError() throws Exception {

        // Given
        // A null session and user not in the cache
        Session nullSession = null;
        User user = user();
        Session notInCacheSession = session;

        // When
        // We put the user's keyring
        keyringCache.remove(nullSession);
        keyringCache.remove(notInCacheSession);

        // Then
        // We should get no error and nothing should be present in the cache
        assertNull(keyringCache.get(user));
    }

    private User user() {
        User result = new User();
        result.setEmail(Random.id() + "@example.com");
        result.resetPassword(Random.password(8));
        return result;
    }
}