package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.model.CollectionTest.createCollection;
import static org.mockito.Mockito.when;

public class ZebedeeCollectionReaderFactoryTest {

    private static final Session SESSION = new Session("1234", "user@example.com");
    private static final String COLLECTION_NAME = "test collection";
    private static final CollectionType TEST_COLLECTION_TYPE = CollectionType.manual;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private Zebedee zebedee;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private CollectionKeyring keyring;

    @Mock
    private UsersService usersService;

    ZebedeeCollectionReaderFactory factory;
    Collection collection;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create test content directories
        temporaryFolder.create();
        Path collectionsPath = temporaryFolder.getRoot().toPath();

        when(zebedee.getPermissionsService())
                .thenReturn(permissionsService);

        when(zebedee.getCollectionKeyring())
                .thenReturn(keyring);

        when(zebedee.getUsersService())
                .thenReturn(usersService);

        // Create a collection instance for use in most tests
        collection = createCollection(collectionsPath, COLLECTION_NAME, zebedee);
        collection.getDescription().setType(TEST_COLLECTION_TYPE);

        factory = new ZebedeeCollectionReaderFactory(zebedee);
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundForNullCollection()
            throws IOException, UnauthorizedException, BadRequestException, NotFoundException {

        // Given a null collection
        Collection collection = null;
        Session session = SESSION;

        // When we attempt to create a collection reader.
        factory.getCollectionReader(collection, session);

        // Then we should get the expected exception, not a null pointer.
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnReadContent()
            throws IOException, UnauthorizedException, BadRequestException, NotFoundException {

        // Given a null session
        Session session = null;

        // When we attempt to create a collection reader.
        factory.getCollectionReader(collection, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }
}
