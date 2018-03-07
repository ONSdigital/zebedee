package com.github.onsdigital.zebedee.permissions.store;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.onsdigital.zebedee.permissions.store.PermissionsStoreFileSystemImpl.ADMINS_KEY;
import static com.github.onsdigital.zebedee.permissions.store.PermissionsStoreFileSystemImpl.COLLECTIONS_KEY;
import static com.github.onsdigital.zebedee.permissions.store.PermissionsStoreFileSystemImpl.DATA_VIS_KEY;
import static com.github.onsdigital.zebedee.permissions.store.PermissionsStoreFileSystemImpl.PERMISSIONS_FILE;
import static com.github.onsdigital.zebedee.permissions.store.PermissionsStoreFileSystemImpl.PUBLISHERS_KEY;
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
    public void initialisePermissions_ShouldCreateAccessMappingIfDoesNotExist() throws Exception {
        PermissionsStoreFileSystemImpl.initialisePermissions(permissionsDir);
        PermissionsStore store = new PermissionsStoreFileSystemImpl(permissionsDir);

        assertThat(Files.exists(accessMappingPath.toPath()), is(true));
        assertThat(new AccessMapping(), equalTo(store.getAccessMapping()));
    }

    @Test
    public void initialisePermissions_ShouldMigrateDataVisUsers() throws Exception {
        accessMappingPath.createNewFile();

        Set<String> digitalPublishingTeam = new HashSet();
        digitalPublishingTeam.add(publisher1);
        digitalPublishingTeam.add(publisher2);
        digitalPublishingTeam.add(publisher3);

        Set<String> dataVisualisationPublishers = new HashSet();
        dataVisualisationPublishers.add(dataVis1);
        dataVisualisationPublishers.add(dataVis2);
        dataVisualisationPublishers.add(dataVis3);

        Map<String, Object> accessMapping = new HashMap();
        accessMapping.put(PUBLISHERS_KEY, digitalPublishingTeam);
        accessMapping.put(DATA_VIS_KEY, dataVisualisationPublishers);
        accessMapping.put(ADMINS_KEY, new HashSet<String>());
        accessMapping.put(COLLECTIONS_KEY, new HashMap<String, Set<Integer>>());

        try (OutputStream output = Files.newOutputStream(accessMappingPath.toPath())) {
            Serialiser.serialise(output, accessMapping);
        }

        AccessMapping expected = new AccessMapping();
        expected.getDigitalPublishingTeam().addAll(dataVisualisationPublishers);
        expected.getDigitalPublishingTeam().addAll(digitalPublishingTeam);

        PermissionsStoreFileSystemImpl.initialisePermissions(permissionsDir);
        PermissionsStore store = new PermissionsStoreFileSystemImpl(permissionsDir);

        assertThat(expected, equalTo(store.getAccessMapping()));
    }

}
