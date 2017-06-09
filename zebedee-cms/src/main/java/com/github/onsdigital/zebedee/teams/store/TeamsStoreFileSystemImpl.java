package com.github.onsdigital.zebedee.teams.store;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.teams.model.Team;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by dave on 08/06/2017.
 */
public class TeamsStoreFileSystemImpl implements TeamsStore {

    private Path teamsPath;
    private ReadWriteLock teamLock = new ReentrantReadWriteLock();

    /**
     * @param teamsPath
     */
    public TeamsStoreFileSystemImpl(Path teamsPath) {
        this.teamsPath = teamsPath;
    }

    @Override
    public Team get(String teamName) throws IOException, NotFoundException {
        Team result = null;

        if (exists(teamName)) {
            // Read the team
            teamLock.readLock().lock();
            try (InputStream input = Files.newInputStream(teamPath(teamName))) {
                result = Serialiser.deserialise(input, Team.class);
            } finally {
                teamLock.readLock().unlock();
            }

            // Initialise the memers set if it's missing:
            if (result.getMembers() == null) {
                result.setMembers(new HashSet<>());
            }

        } else {
            throw new NotFoundException("Team not found: " + teamName);
        }
        return result;
    }

    @Override
    public void save(Team team) throws IOException, NotFoundException {
        Path path = teamPath(team);

        if (path != null) {
            teamLock.writeLock().lock();
            try (OutputStream output = Files.newOutputStream(path)) {
                Serialiser.serialise(output, team);
            } finally {
                teamLock.writeLock().unlock();
            }
        } else {
            throw new NotFoundException("Team not found: " + team);
        }
    }

    @Override
    public List<Team> listTeams() throws IOException {
        List<Team> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(teamsPath)) {
            for (Path path : stream) {
                if (path.getFileName().toString().equalsIgnoreCase(".DS_Store")) {
                    continue;
                }
                teamLock.readLock().lock();
                try (InputStream input = Files.newInputStream(path)) {
                    result.add(Serialiser.deserialise(input, Team.class));
                } finally {
                    teamLock.readLock().unlock();
                }
            }
        }
        return result;
    }

    @Override
    public boolean exists(String teamName) throws IOException {
        Path path = teamPath(teamName);
        return path != null && Files.exists(path);
    }

    @Override
    public boolean deleteTeam(Team target) throws IOException, NotFoundException {
        if (!exists(target.getName())) {
            throw new NotFoundException("Team ID not found: " + target.getId());
        }
        return Files.deleteIfExists(teamPath(target.getName()));
    }

    private Path teamPath(Team team) {
        Path result = null;
        if (team != null) {
            result = teamPath(team.getName());
        }
        return result;
    }


    private Path teamPath(String teamName) {
        Path result = null;
        if (StringUtils.isNotBlank(teamName)) {
            result = teamsPath.resolve(PathUtils.toFilename(teamName) + ".json");
        }
        return result;
    }

}
