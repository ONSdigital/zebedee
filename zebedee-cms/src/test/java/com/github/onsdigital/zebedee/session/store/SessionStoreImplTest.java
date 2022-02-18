package com.github.onsdigital.zebedee.session.store;

import com.github.onsdigital.zebedee.TestUtils;
import com.github.onsdigital.zebedee.json.serialiser.IsoDateSerializer;
import com.github.onsdigital.zebedee.session.model.LegacySession;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class SessionStoreImplTest {

    @Rule
    public TemporaryFolder root = new TemporaryFolder();

    private SessionsStore sessionsStore;
    private File sessionsDir;
    private Date lastAccessed;
    private Date startDate;
    private LegacySession session;
    private Path sessionFile;
    private GsonBuilder gsonBuilder;

    @BeforeClass
    public static void setup() {
        TestUtils.initReaderConfig();
    }

    @AfterClass
    public static void staticTearDown() {
        TestUtils.clearReaderConfig();
    }

    @Before
    public void setUp() throws Exception {
        root.create();
        sessionsDir = root.newFolder("sessions");
        lastAccessed = new Date();
        startDate = new Date();

        session = new LegacySession("666", "test@ons.gov.co.uk");

        String fileName = UUID.randomUUID().toString();
        sessionFile = sessionsDir.toPath().resolve(fileName + ".json");

        sessionsStore = new SessionsStoreImpl(sessionsDir.toPath());

        gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Date.class, new IsoDateSerializer());
    }

    @After
    public void tearDown() throws Exception {
        root.delete();
    }

    @Test
    public void filterSessions_Success() throws Exception {
        sessionFile.toFile().createNewFile();
        Files.write(sessionFile, gsonBuilder.create().toJson(session).getBytes(), StandardOpenOption.APPEND);

        List<LegacySession> result = sessionsStore.filterSessions((p) -> true); // return all / any session.
        assertThat(result.size(), equalTo(1));
        assertEquals(session.getId(), result.get(0).getId());
        assertEquals(session.getEmail(), result.get(0).getEmail());
    }

    @Test
    public void filterSessions_ShouldIgnoreAnyNonJSONFiles() throws Exception {
        // create some non json files.
        sessionsDir.toPath().resolve(".DS_Store").toFile().createNewFile();
        sessionsDir.toPath().resolve("shoppinglist.txt").toFile().createNewFile();
        sessionFile.toFile().createNewFile();
        Files.write(sessionFile, gsonBuilder.create().toJson(session).getBytes(), StandardOpenOption.APPEND);

        List<LegacySession> result = sessionsStore.filterSessions((p) -> true); // return all / any session.
        assertThat(result.size(), equalTo(1));
        assertEquals(session.getId(), result.get(0).getId());
        assertEquals(session.getEmail(), result.get(0).getEmail());
    }

    @Test
    public void find_Success() throws Exception {
        sessionFile.toFile().createNewFile();
        Files.write(sessionFile, gsonBuilder.create().toJson(session).getBytes(), StandardOpenOption.APPEND);

        LegacySession result = sessionsStore.find(session.getEmail()); // return all / any session.
        assertEquals(session.getId(), result.getId());
        assertEquals(session.getEmail(), result.getEmail());
    }

    @Test
    public void find_ShouldIgnoreAnyNonJSONFiles() throws Exception {
        // create some non json files.
        sessionsDir.toPath().resolve(".DS_Store").toFile().createNewFile();
        sessionsDir.toPath().resolve("shoppinglist.txt").toFile().createNewFile();
        sessionFile.toFile().createNewFile();
        Files.write(sessionFile, gsonBuilder.create().toJson(session).getBytes(), StandardOpenOption.APPEND);

        LegacySession result = sessionsStore.find(session.getEmail()); // return all / any session.
        assertEquals(session.getId(), result.getId());
        assertEquals(session.getEmail(), result.getEmail());
    }
}
