package com.github.onsdigital.zebedee.teams.store;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.util.serialiser.JSONSerialiser;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.apache.commons.io.FilenameUtils.isExtension;


public class TeamsStoreFileSystemImpl implements TeamsStore {

    private static final String JSON_EXT = ".json";

    private Path teamsPath;
    private ReadWriteLock teamLock = new ReentrantReadWriteLock();
    private JSONSerialiser<Team> teamJSONSerialiser;

    /**
     * Return true if the {@link Path} is not null and ends with '.json'.
     */
    private Predicate<Path> jsonEXTFilter = (p) -> p != null && isExtension(p.getFileName().toString(), "json");

    /**
     * @param teamsPath
     */
    public TeamsStoreFileSystemImpl(Path teamsPath) {
        this.teamsPath = teamsPath;
        this.teamJSONSerialiser = new JSONSerialiser(Team.class);
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

        teamLock.readLock().lock();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(teamsPath)) {
            List<Path> teams = StreamSupport
                    .stream(stream.spliterator(), true)
                    .filter(p -> jsonEXTFilter.test(p))
                    .collect(Collectors.toList());

            for (Path path : teams) {
                try (InputStream input = Files.newInputStream(path)) {
                    Team t = teamJSONSerialiser.deserialiseQuietly(input, path);
                    if (t != null) {
                        result.add(t);
                    }
                }
            }
        } finally {
            teamLock.readLock().unlock();
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
            result = teamsPath.resolve(PathUtils.toFilename(teamName) + JSON_EXT);
        }
        return result;
    }

}
