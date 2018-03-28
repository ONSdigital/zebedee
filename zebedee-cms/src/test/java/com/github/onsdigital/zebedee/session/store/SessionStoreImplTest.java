package com.github.onsdigital.zebedee.session.store;

import com.github.onsdigital.zebedee.session.model.Session;
import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
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

public class SessionStoreImplTest {

    @Rule
    public TemporaryFolder root = new TemporaryFolder();

    private SessionsStore sessionsStore;
    private File sessionsDir;
    private Date lastAccessed;
    private Date startDate;
    private Session session;
    private Path sessionFile;

    @Before
    public void setUp() throws Exception {
        root.create();
        sessionsDir = root.newFolder("sessions");
        lastAccessed = new Date();
        startDate = new Date();

        session = new Session();
        session.setEmail("test@ons.gov.co.uk");
        session.setId("666");
        session.setLastAccess(null);
        session.setStart(null);

        String fileName = UUID.randomUUID().toString();
        sessionFile = sessionsDir.toPath().resolve(fileName + ".json");

        sessionsStore = new SessionsStoreImpl(sessionsDir.toPath());
    }

    @After
    public void tearDown() throws Exception {
        root.delete();
    }

    @Test
    public void filterSessions_Success() throws Exception {
        sessionFile.toFile().createNewFile();
        Files.write(sessionFile, new Gson().toJson(session).getBytes(), StandardOpenOption.APPEND);

        List<Session> result = sessionsStore.filterSessions((p) -> true); // return all / any session.
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0), equalTo(session));
    }

    @Test
    public void filterSessions_ShouldIgnoreAnyNonJSONFiles() throws Exception {
        // create some non json files.
        sessionsDir.toPath().resolve(".DS_Store").toFile().createNewFile();
        sessionsDir.toPath().resolve("shoppinglist.txt").toFile().createNewFile();
        sessionFile.toFile().createNewFile();
        Files.write(sessionFile, new Gson().toJson(session).getBytes(), StandardOpenOption.APPEND);

        List<Session> result = sessionsStore.filterSessions((p) -> true); // return all / any session.
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0), equalTo(session));
    }

    @Test
    public void find_Success() throws Exception {
        sessionFile.toFile().createNewFile();
        Files.write(sessionFile, new Gson().toJson(session).getBytes(), StandardOpenOption.APPEND);

        Session result = sessionsStore.find(session.getEmail()); // return all / any session.
        assertThat(result, equalTo(session));
    }

    @Test
    public void find_ShouldIgnoreAnyNonJSONFiles() throws Exception {
        // create some non json files.
        sessionsDir.toPath().resolve(".DS_Store").toFile().createNewFile();
        sessionsDir.toPath().resolve("shoppinglist.txt").toFile().createNewFile();
        sessionFile.toFile().createNewFile();
        Files.write(sessionFile, new Gson().toJson(session).getBytes(), StandardOpenOption.APPEND);

        Session result = sessionsStore.find(session.getEmail()); // return all / any session.
        assertThat(result, equalTo(session));
    }
}
