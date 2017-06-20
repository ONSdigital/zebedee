package com.github.onsdigital.zebedee.user.store;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;
import com.github.onsdigital.zebedee.user.model.UserListCollector;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * Created by dave on 30/05/2017.
 */
public class UserStoreFileSystemImpl implements UserStore {

    private static final String JSON_EXT = ".json";
    private static final Path DS_STORE = Paths.get(".DS_Store");

    private Path usersPath;

    private Function<Path, User> userDeserializer = (p) -> {
        try {
            return Serialiser.deserialise(p, User.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    public UserStoreFileSystemImpl(Path usersPath) {
        this.usersPath = usersPath;
    }

    @Override
    public boolean exists(String email) throws IOException {
        return StringUtils.isNotBlank(email) && Files.exists(userPath(email));
    }


    @Override
    public User get(String email) throws IOException {
        User result = null;
        if (exists(email)) {
            result = Serialiser.deserialise(userPath(email), User.class);
        }
        return result;
    }

    @Override
    public void save(User user) throws IOException {
        user.setEmail(normalise(user.getEmail()));
        Path userPath = userPath(user.getEmail());
        Serialiser.serialise(userPath, user);
    }

    @Override
    public UserList list() throws IOException {
        return StreamSupport.stream(Files.newDirectoryStream(usersPath).spliterator(), false)
                .filter(path -> !Files.isDirectory(path) && !path.getFileName().equals(DS_STORE))
                .map(userPath -> {
                    return userDeserializer.apply(userPath);
                }).collect(new UserListCollector());
    }

    @Override
    public boolean delete(User user) throws IOException, UnauthorizedException, NotFoundException {
        return Files.deleteIfExists(userPath(user.getEmail()));
    }

    private Path userPath(String email) {
        Path result = null;
        if (StringUtils.isNotBlank(email)) {
            String userFileName = PathUtils.toFilename(normalise(email));
            userFileName += JSON_EXT;
            result = usersPath.resolve(userFileName);
        }
        return result;
    }

    private String normalise(String email) {
        return StringUtils.lowerCase(StringUtils.trim(email));
    }
}
