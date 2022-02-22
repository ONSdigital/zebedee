package com.github.onsdigital.zebedee.permissions.store;

import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.permissions.store.PermissionsStoreFileSystemImpl.PERMISSIONS_FILE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PermissionsStoreFileSystemImplTest {

    private static final String publisher1 = "publisher1@ons.gov.uk";
    private static final String publisher2 = "publisher2@ons.gov.uk";
    private static final String publisher3 = "publisher3@ons.gov.uk";

    private static final String dataVis1 = "dataVis1@ons.gov.uk";
    private static final String dataVis2 = "dataVis2@ons.gov.uk";
    private static final String dataVis3 = "dataVis3@ons.gov.uk";

    @Rule
    public TemporaryFolder root = new TemporaryFolder();

    private Path permissionsDir;
    private File accessMappingPath;

    @Before
    public void setUp() throws Exception {
        root.create();
        permissionsDir = root.newFolder("permissions").toPath();
        accessMappingPath = permissionsDir.resolve(PERMISSIONS_FILE).toFile();
    }

    @After
    public void tearDown() throws Exception {
        root.delete();
    }

    @Test
    public void constructor_ShouldCreateAccessMappingIfDoesNotExist() throws Exception {
        PermissionsStoreFileSystemImpl.initialiseAccessMapping(permissionsDir);
        PermissionsStore store = new PermissionsStoreFileSystemImpl(permissionsDir);

        assertThat(Files.exists(accessMappingPath.toPath()), is(true));
        assertThat(new AccessMapping(), equalTo(store.getAccessMapping()));
    }
}
