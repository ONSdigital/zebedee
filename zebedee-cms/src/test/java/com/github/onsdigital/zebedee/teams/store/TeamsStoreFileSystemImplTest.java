package com.github.onsdigital.zebedee.teams.store;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.teams.service.TeamsServiceImpl;
import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import static com.github.onsdigital.zebedee.Zebedee.TEAMS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests verifing the behaviour of the {@link TeamsServiceImpl} in both success and failure scenarios.
 */
public class TeamsStoreFileSystemImplTest {

    private static final String TEST_EMAIL = "test@ons.gov.uk";

    @Mock
    private ReadWriteLock rwLock;

    @Mock
    private Lock lock;

    @Rule
    public TemporaryFolder zebedeeRoot;

    private TeamsStore store;
    private Path teamsPath;
    private Team teamA;
    private Team teamB;

    /**
     * Set up the test.
     */
    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        zebedeeRoot = new TemporaryFolder();
        zebedeeRoot.create();
        zebedeeRoot.newFolder("teams");
        teamsPath = zebedeeRoot.getRoot().toPath().resolve(TEAMS);

        store = new TeamsStoreFileSystemImpl(teamsPath);

        teamA = new Team()
                .setId(123)
                .setName("Team-A")
                .setMembers(new HashSet<>())
                .addMember("Dave")
                .addMember("Adrian")
                .addMember("Janick");

        teamB = new Team()
                .setId(456)
                .setName("Team-B")
                .setMembers(new HashSet<>())
                .addMember("Bruce")
                .addMember("Steve")
                .addMember("Nicko");

        when(rwLock.readLock()).thenReturn(lock);
        when(rwLock.writeLock()).thenReturn(lock);
    }

    /**
     * Clean up.
     */
    @After
    public void cleanUp() {
        zebedeeRoot.delete();
    }

    @Test
    public void getTeam_Success() throws Exception {
        createTeams();

        ReflectionTestUtils.setField(store, "teamLock", rwLock);

        Team result = store.get(teamA.getName());
        assertThat(result, equalTo(teamA));
        verify(rwLock, times(2)).readLock();
        verify(lock, times(1)).lock();
        verify(lock, times(1)).unlock();
    }

    @Test(expected = NotFoundException.class)
    public void getTeam_ShouldThrowExIfTeamNotFound() throws Exception {
        ReflectionTestUtils.setField(store, "teamLock", rwLock);

        try {
            store.get(teamA.getName());
        } catch (NotFoundException e) {
            verifyZeroInteractions(rwLock, lock);
            throw e;
        }
    }

    @Test
    public void save_Success() throws Exception {
        ReflectionTestUtils.setField(store, "teamLock", rwLock);

        assertThat(teamsPath.toFile().list().length, equalTo(0));

        store.save(teamA);

        assertThat(teamsPath.toFile().list().length, equalTo(1));
        assertThat(teamA, equalTo(getTeam(teamA.getName())));
        verify(rwLock, times(2)).writeLock();
        verify(lock, times(1)).lock();
        verify(lock, times(1)).unlock();
    }

    @Test(expected = NotFoundException.class)
    public void save_ShouldThrowExIfTeamNull() throws Exception {
        // verify there are no Teams before the test.
        assertThat(teamsPath.toFile().list().length, equalTo(0));

        try {
            store.save(null);
        } catch (NotFoundException e) {
            assertThat(teamsPath.toFile().list().length, equalTo(0));
            verifyZeroInteractions(rwLock, lock);
            throw e;
        }
    }

    @Test
    public void listTeams_Success() throws Exception {
        ReflectionTestUtils.setField(store, "teamLock", rwLock);

        List<Team> expected = createTeams();

        assertThat(store.listTeams(), equalTo(expected));
        verify(rwLock, times(2)).readLock();
        verify(lock, times(1)).lock();
        verify(lock, times(1)).unlock();
    }

    @Test
    public void exists_ShouldReturnFalseIfPathNull() throws Exception {
        assertThat(store.exists(null), is(false));
    }

    @Test
    public void exists_ShouldReturnFalseIfTeamDoesNotExist() throws Exception {
        assertThat(teamsPath.toFile().list().length, equalTo(0));
        assertThat(store.exists(null), is(false));
    }

    @Test
    public void exists_ShouldReturnTrueIfTeamExists() throws Exception {
        createTeams();
        assertThat(store.exists(teamA.getName()), is(true));
    }

    @Test
    public void delete_ShouldDeleteIfExists() throws Exception {
        createTeams();
        assertThat(Files.exists(toPath(teamB.getName())), is(true));
        assertThat(Files.exists(toPath(teamA.getName())), is(true));
        assertThat(teamsPath.toFile().list().length, equalTo(2));

        assertThat(store.deleteTeam(teamA), is(true));

        assertThat(Files.exists(toPath(teamB.getName())), is(true));
        assertThat(Files.exists(toPath(teamA.getName())), is(false));
        assertThat(teamsPath.toFile().list().length, equalTo(1));
    }


    private void writeTeams(List<Team> teanList) throws IOException {
        for (Team t : teanList) {
            File f = toPath(t.getName()).toFile();
            f.createNewFile();

            try (FileOutputStream fos = new FileOutputStream(f, false)) {
                fos.write(new Gson().toJson(t).getBytes());
                fos.flush();
            }
        }
    }

    private Path toPath(String name) {
        return teamsPath.resolve(name.replace("-", "").toLowerCase().trim() + ".json");
    }

    private Team getTeam(String name) throws IOException {
        try (InputStream input = Files.newInputStream(toPath(name))) {
            return Serialiser.deserialise(input, Team.class);
        }
    }

    private List<Team> createTeams() throws IOException {
        List<Team> teamsList = new ArrayList<>();
        teamsList.add(teamA);
        teamsList.add(teamB);
        writeTeams(teamsList);
        return teamsList;
    }
}
