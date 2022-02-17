package com.github.onsdigital.zebedee.user.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.user.model.AdminOptions;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;
import com.github.onsdigital.zebedee.user.store.UserStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static com.github.onsdigital.zebedee.user.service.UsersServiceImpl.SYSTEM_USER;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test verifying the behaviour of the {@link UsersServiceImpl}.
 */
public class UsersServiceTest {

    private static final String EMAIL = "test@ons.gov.uk";
    private static final String EMAIL_2 = "test2@ons.gov.uk";
    private static final String KEY_IDENTIFIER = "Valar morghulis";
    private static final String MOCK_USER_NAME = "A girl is no one";

    @Mock
    private Collections collections;

    @Mock
    private PermissionsService permissions;

    @Mock
    private UserStore userStore;

    @Mock
    private ReentrantLock lockMock;

    @Mock
    private Session session;

    @Mock
    private User userMock;

    @Mock
    private UsersServiceImpl.UserFactory userFactory;

    @Mock
    private CollectionKeyring collectionKeyring;

    private UsersService service;
    private User user;
    private Supplier<CollectionKeyring> keyringSupplier;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        user = new User();
        user.setEmail(EMAIL);
        user.setName("JIM");
        user.setInactive(false);
        user.setTemporaryPassword(false);
        user.setAdminOptions(new AdminOptions());

        service = new UsersServiceImpl(userStore, permissions);

        when(userMock.getEmail())
                .thenReturn(EMAIL_2);
        when(userMock.getName())
                .thenReturn(MOCK_USER_NAME);

        ReflectionTestUtils.setField(service, "lock", lockMock);
    }

    private void verifyLockObtainedAndReleased() {
        verify(lockMock, times(1)).lock();
        verify(lockMock, times(1)).unlock();
    }

    @Test(expected = BadRequestException.class)
    public void getUserByEmail_ShouldThrowErrorForEmailNull() throws Exception {
        try {
            service.getUserByEmail(null);
        } catch (BadRequestException e) {
            verify(userStore, never()).exists(anyString());
            verify(userStore, never()).get(anyString());
            verifyLockObtainedAndReleased();
            throw e;
        }
    }

    @Test(expected = NotFoundException.class)
    public void getUserByEmail_ShouldThrowErrorIfUserDoesNotExist() throws Exception {
        when(userStore.exists(EMAIL))
                .thenReturn(false);

        try {
            service.getUserByEmail(EMAIL);
        } catch (NotFoundException e) {
            verify(userStore, times(1)).exists(EMAIL);
            verify(userStore, never()).get(anyString());
            verifyLockObtainedAndReleased();
            throw e;
        }
    }

    @Test
    public void getUserByEmail_Success() throws Exception {
        when(userStore.exists(EMAIL))
                .thenReturn(true);
        when(userStore.get(EMAIL))
                .thenReturn(user);

        User result = service.getUserByEmail(EMAIL);

        assertThat(result, equalTo(user));
        verify(userStore, times(1)).exists(EMAIL);
        verify(userStore, times(1)).get(EMAIL);
        verifyLockObtainedAndReleased();
    }

    @Test(expected = IOException.class)
    public void getUserByEmail_ShouldReleaseLockIfErrorIsThrown() throws Exception {
        when(userStore.exists(EMAIL))
                .thenReturn(true);
        when(userStore.get(EMAIL))
                .thenThrow(new IOException());

        try {
            service.getUserByEmail(EMAIL);
        } catch (IOException e) {
            verify(userStore, times(1)).exists(EMAIL);
            verify(userStore, times(1)).get(EMAIL);
            verifyLockObtainedAndReleased();
            throw e;
        }
    }

    @Test
    public void exists_ShouldCheckIfUserExistsByEmail() throws Exception {
        when(userStore.exists(EMAIL))
                .thenReturn(true);

        assertThat(service.exists(EMAIL), is(true));
        verify(userStore, times(1)).exists(EMAIL);
    }

    @Test(expected = UnauthorizedException.class)
    public void create_ShouldNotCreateUserIfIsNotAdmin() throws Exception {
        when(permissions.isAdministrator(session))
                .thenReturn(false);
        try {
            service.create(session, user);
        } catch (UnauthorizedException e) {
            verify(permissions, times(1)).isAdministrator(session);
            verify(userStore, never()).exists(anyString());
            verify(userStore, never()).save(any(User.class));
            verifyZeroInteractions(lockMock);
            throw e;
        }
    }

    @Test(expected = BadRequestException.class)
    public void create_ShouldNotCreateUserIfUserIsNull() throws Exception {
        when(permissions.isAdministrator(session))
                .thenReturn(true);

        try {
            service.create(session, null);
        } catch (BadRequestException e) {
            verify(permissions, times(1)).isAdministrator(session);
            verify(userStore, never()).exists(anyString());
            verify(userStore, never()).save(any(User.class));
            verifyZeroInteractions(lockMock);
            throw e;
        }
    }

    @Test(expected = ConflictException.class)
    public void create_ShouldNotCreateUserIfUserAlreadyExists() throws Exception {
        when(permissions.isAdministrator(session))
                .thenReturn(true);
        when(userStore.exists(EMAIL))
                .thenReturn(true);

        try {
            service.create(session, user);
        } catch (ConflictException e) {
            verify(permissions, times(1)).isAdministrator(session);
            verify(userStore, times(1)).exists(EMAIL);
            verify(userStore, never()).save(any(User.class));
            verifyZeroInteractions(lockMock);
            throw e;
        }
    }

    @Test(expected = BadRequestException.class)
    public void create_ShouldNotCreateUserIfUserInvalid() throws Exception {
        // Null is not valid user.name
        user.setName(null);

        when(permissions.isAdministrator(session))
                .thenReturn(true);
        when(userStore.exists(EMAIL))
                .thenReturn(false);
        try {
            service.create(session, user);
        } catch (BadRequestException e) {
            verify(permissions, times(1)).isAdministrator(session);
            verify(userStore, times(1)).exists(EMAIL);
            verify(userStore, never()).save(any(User.class));
            verifyZeroInteractions(lockMock);
            throw e;
        }
    }

    @Test
    public void create_Success() throws Exception {
        when(permissions.isAdministrator(session))
                .thenReturn(true);
        when(userStore.exists(EMAIL))
                .thenReturn(false);
        when(session.getEmail())
                .thenReturn(EMAIL);

        User result = service.create(session, user);

        User expected = new User();
        expected.setName(user.getName());
        expected.setEmail(user.getEmail());
        expected.setInactive(true);
        expected.setTemporaryPassword(true);
        expected.setLastAdmin(EMAIL);

        assertThat(expected, equalTo(result));

        verify(permissions, times(1)).isAdministrator(session);
        verify(userStore, times(2)).exists(EMAIL);
        verify(userStore, times(1)).save(expected);
        verify(session, times(1)).getEmail();
        verify(userStore, times(1)).save(any(User.class));
        verifyLockObtainedAndReleased();
    }

    @Test
    public void list_Success() throws Exception {
        UserList ul = new UserList();
        ul.add(user);

        when(userStore.list())
                .thenReturn(ul);

        assertThat(ul, equalTo(service.list()));
        verify(userStore, times(1)).list();
    }

    @Test(expected = UnauthorizedException.class)
    public void update_ShouldThrowExceptionIfNotAuthorized() throws Exception {
        when(session.getEmail())
                .thenReturn(EMAIL);
        when(permissions.isAdministrator(session))
                .thenReturn(false);

        try {
            service.update(session, null, null);
        } catch (UnauthorizedException e) {
            verify(permissions, times(1)).isAdministrator(session);
            verifyZeroInteractions(lockMock, userStore);
            throw e;
        }
    }

    @Test(expected = NotFoundException.class)
    public void update_ShouldThrowExceptionIfUserDoesNotExist() throws Exception {
        when(permissions.isAdministrator(session))
                .thenReturn(true);
        when(userStore.exists(EMAIL_2))
                .thenReturn(false);

        try {
            service.update(session, userMock, null);
        } catch (NotFoundException e) {
            verify(permissions, times(1)).isAdministrator(session);
            verify(userStore, times(1)).exists(EMAIL_2);
            verifyNoMoreInteractions(userStore);
            verifyZeroInteractions(lockMock);
            throw e;
        }
    }

    @Test
    public void update_Success() throws Exception {
        when(permissions.isAdministrator(session))
                .thenReturn(true);
        when(userStore.exists(EMAIL))
                .thenReturn(true);
        when(session.getEmail())
                .thenReturn(EMAIL);

        User updated = new User();
        updated.setEmail(EMAIL);
        updated.setName("Bob");
        updated.setInactive(false);
        updated.setTemporaryPassword(false);
        updated.setLastAdmin(EMAIL);
        updated.setAdminOptions(new AdminOptions());

        User result = service.update(session, user, updated);

        verify(permissions, times(1)).isAdministrator(session);
        verify(userStore, times(1)).exists(EMAIL);
        verify(userStore, times(1)).save(updated);
        verifyLockObtainedAndReleased();
    }

    @Test(expected = UnauthorizedException.class)
    public void delete_ShouldThrowExceptionIfNotAuthorized() throws Exception {
        when(permissions.isAdministrator(session))
                .thenReturn(false);

        try {
            service.delete(session, user);
        } catch (UnauthorizedException e) {
            verify(permissions, times(1)).isAdministrator(session);
            verifyZeroInteractions(userStore, lockMock);
            throw e;
        }
    }

    @Test(expected = BadRequestException.class)
    public void delete_ShouldThrowExceptionIfSessionIsNull() throws Exception {
        try {
            service.delete(null, user);
        } catch (BadRequestException e) {
            verifyZeroInteractions(permissions, userStore, lockMock);
            throw e;
        }
    }

    @Test(expected = NotFoundException.class)
    public void delete_ShouldThrowExceptionIfUserDoesNotExist() throws Exception {
        when(permissions.isAdministrator(session))
                .thenReturn(true);
        when(userStore.exists(user.getEmail()))
                .thenReturn(false);

        try {
            service.delete(session, user);
        } catch (NotFoundException e) {
            verify(permissions, times(1)).isAdministrator(session);
            verify(userStore, times(1)).exists(user.getEmail());
            verify(userStore, never()).delete(any(User.class));
            verifyZeroInteractions(lockMock);
            throw e;
        }
    }

    @Test
    public void delete_Success() throws Exception {
        when(permissions.isAdministrator(session))
                .thenReturn(true);
        when(userStore.exists(user.getEmail()))
                .thenReturn(true);
        when(userStore.delete(user))
                .thenReturn(true);

        assertThat(service.delete(session, user), is(true));
        verify(permissions, times(1)).isAdministrator(session);
        verify(userStore, times(1)).exists(user.getEmail());
        verify(userStore, times(1)).delete(user);
    }

    @Test
    public void createSystemUser_Success() throws Exception {
        String password = "Valar morghulis";

        User expected = new User();
        expected.setEmail(EMAIL_2);
        expected.setName(MOCK_USER_NAME);
        expected.setInactive(true);
        expected.setTemporaryPassword(true);
        expected.setLastAdmin(SYSTEM_USER);

        ReflectionTestUtils.setField(service, "userFactory", userFactory);

        when(permissions.hasAdministrator())
                .thenReturn(false);
        when(userStore.exists(MOCK_USER_NAME))
                .thenReturn(false);
        when(userFactory.newUserWithDefaultSettings(EMAIL_2, MOCK_USER_NAME, SYSTEM_USER))
                .thenReturn(userMock);

        service.createSystemUser(userMock, password);

        verify(permissions, times(1)).hasAdministrator();
        verify(userStore, times(1)).exists(EMAIL_2);
        verify(userStore, times(2)).save(userMock);
        verify(userMock, times(1)).resetPassword(password);
        verify(userMock, times(1)).setInactive(false);
        verify(userMock, times(1)).setLastAdmin(SYSTEM_USER);
        verify(userMock, times(1)).setTemporaryPassword(true);
        verify(permissions, times(1)).addEditor(EMAIL_2, null);
        verify(permissions, times(1)).addAdministrator(EMAIL_2, null);
        verify(lockMock, times(3)).lock();
        verify(lockMock, times(3)).unlock();
    }

    @Test
    public void createSystemUser_ShouldNotCreateIfAlreadyExists() throws Exception {
        when(permissions.hasAdministrator())
                .thenReturn(true);

        service.createSystemUser(userMock, null);

        verify(permissions, times(1)).hasAdministrator();
        verify(userStore, never()).exists(anyString());
        verify(userStore, never()).save(any(User.class));
        verify(userMock, never()).resetPassword(anyString());
        verify(userMock, never()).setInactive(anyBoolean());
        verify(userMock, never()).setLastAdmin(anyString());
        verify(userMock, never()).setTemporaryPassword(anyBoolean());
        verify(permissions, never()).addEditor(anyString(), any(Session.class));
        verify(permissions, never()).addAdministrator(anyString(), any(Session.class));
        verify(lockMock, never()).lock();
        verify(lockMock, never()).unlock();
    }

    @Test
    public void setPasswod_success() throws Exception {
        String email = "test1@ons.gov.uk";

        Credentials credentials = new Credentials();
        credentials.setEmail(email);
        credentials.setPassword("a b c d");

        when(userStore.exists(email))
                .thenReturn(true);

        when(userStore.get(email))
                .thenReturn(userMock);

        when(permissions.isAdministrator(session))
                .thenReturn(true);

        when(permissions.isAdministrator(session))
                .thenReturn(true);

        User adminUser = mock(User.class);
        when(userStore.get(session.getEmail()))
                .thenReturn(adminUser);

        Set<String> collectionIds = new HashSet<>();
        collectionIds.add("abc");
        collectionIds.add("def");

        Collections.CollectionList allCollections = new Collections.CollectionList();
        Collection c1 = mock(Collection.class);
        when(c1.getId())
                .thenReturn("abc");

        CollectionDescription desc1 = mock(CollectionDescription.class);
        when(c1.getDescription())
                .thenReturn(desc1);
        allCollections.add(c1);

        Collection c2 = mock(Collection.class);
        when(c2.getId())
                .thenReturn("def");

        CollectionDescription desc2 = mock(CollectionDescription.class);
        when(c2.getDescription())
                .thenReturn(desc2);
        allCollections.add(c2);

        when(collections.list())
                .thenReturn(allCollections);

        boolean result = service.setPassword(session, credentials);

        assertTrue(result);

        List<CollectionDescription> expected = new ArrayList<>();
        expected.add(desc1);
        expected.add(desc2);
        verify(userStore, times(1)).save(userMock);
    }

}
