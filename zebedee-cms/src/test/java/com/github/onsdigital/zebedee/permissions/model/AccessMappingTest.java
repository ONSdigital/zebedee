package com.github.onsdigital.zebedee.permissions.model;

import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import org.hamcrest.CoreMatchers;import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import java.io.File;
import java.nio.file.Files;import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;import java.util.List;import java.util.Map;
import java.util.Set;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class AccessMappingTest {
    private static final String COLLECTION_ID = "1234";
    private static final int COLLECTION_ID_int = 1234;

    static final String PERMISSIONS_FILE = "accessMapping.json";

    @Mock
    private AccessMapping accessMapping;

    @Rule
    public org.junit.rules.TemporaryFolder root = new org.junit.rules.TemporaryFolder();

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
    public void getAdministrators_return_empty() throws Exception {
        AccessMapping accessMapping = new AccessMapping();
        Set<String> result = accessMapping.getAdministrators();
        assertTrue(result.isEmpty());
    }

    @Test
    public void getDigitalPublishingTeam_empty() throws Exception {
        AccessMapping accessMapping = new AccessMapping();
        Set<String> result = accessMapping.getDigitalPublishingTeam();
        assertTrue(result.isEmpty());
    }

    @Test
    public void getCollections_empty() throws Exception {
        AccessMapping accessMapping = new AccessMapping();
        Map<String, java.util.Set<String>> result = accessMapping.getCollections();
        assertTrue(result.isEmpty());
    }

    @Test
    public void getAdministrators_return_set() throws Exception {
        AccessMapping accessMapping = new AccessMapping();
        Set<String> adminList = new HashSet<String>() {{
            add("123");
            add("456");
        }};
        accessMapping.setAdministrators(adminList);
        Set<String> result = accessMapping.getAdministrators();
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(c -> c.equals("123")));
        assertTrue(result.stream().anyMatch(c -> c.equals("456")));
    }

    @Test
    public void getDigitalPublishingTeam_return_set() throws Exception {
        AccessMapping accessMapping = new AccessMapping();
        Set<String> digitalTeamList = new HashSet<String>() {{
            add("123");
            add("456");
        }};
        accessMapping.setDigitalPublishingTeam(digitalTeamList);
        Set<String> result = accessMapping.getDigitalPublishingTeam();
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(c -> c.equals("123")));
        assertTrue(result.stream().anyMatch(c -> c.equals("456")));
    }

    @Test
    public void getDigitalPublishingTeam_return_true() throws Exception {
        AccessMapping accessMapping = new AccessMapping();
        Map<String, Set<String>> collectionMapping = new HashMap<>();

        Set<String> originalList = new HashSet<String>() {{
            add("123456");
            add("789012345");
        }};

        collectionMapping.put(COLLECTION_ID, originalList);

        accessMapping.setCollections(collectionMapping);
        Map<String, Set<String>> result = accessMapping.getCollections();
        assertThat(result, is(notNullValue()));
        assertTrue(result.get(COLLECTION_ID).size() > 0);
        assertTrue(result.get(COLLECTION_ID).contains("789012345"));
        assertTrue(result.get(COLLECTION_ID).contains("123456"));
    }

/**
*
* due to migration to identity-api collection team ids change from int to string
* this test is to ensure that legacy collections will still work in the migrated system
*
 @throws Exception
*/
    @Test
    public void importIntAccessMapping() throws Exception {
        PermissionsStore store = new com.github.onsdigital.zebedee.permissions.store.PermissionsStoreFileSystemImpl(permissionsDir);
        AccessMapping accessMapping = new AccessMapping();
        Map<String, Set<String>> collectionMapping = new HashMap<>();
        HashSet teamList = new HashSet<Integer>() {{add(1);add(2);}};
        collectionMapping.put(COLLECTION_ID, teamList);
        accessMapping.setCollections(collectionMapping);

        Map<String, Set<String>> result1 = accessMapping.getCollections();

        assertThat(result1, is(notNullValue()));
        assertTrue(result1.get(COLLECTION_ID).size() > 0);
        assertTrue(result1.get(COLLECTION_ID).contains(1));
        assertTrue(result1.get(COLLECTION_ID).contains(2));

        store.saveAccessMapping(accessMapping);

        assertThat(Files.exists(accessMappingPath.toPath()), CoreMatchers.is(true));

        AccessMapping input = store.getAccessMapping();
        Map<String, Set<String>> result = input.getCollections();

        assertThat(result, is(notNullValue()));
        assertTrue(result.get(COLLECTION_ID).size() > 0);
        assertTrue(result.get(COLLECTION_ID).contains("1"));
        assertTrue(result.get(COLLECTION_ID).contains("2"));
        }

}
