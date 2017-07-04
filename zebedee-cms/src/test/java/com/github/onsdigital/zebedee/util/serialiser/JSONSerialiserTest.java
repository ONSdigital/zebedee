package com.github.onsdigital.zebedee.util.serialiser;

import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.util.serialiser.JSONSerialiser;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by dave on 04/07/2017.
 */
public class JSONSerialiserTest {

    @Rule
    public TemporaryFolder root = new TemporaryFolder();

    private JSONSerialiser<User> serialiser;
    private File usersDir;

    @Before
    public void setUp() throws Exception {
        root.create();
        usersDir = root.newFolder("user");
        serialiser = new JSONSerialiser(User.class);
    }

    @After
    public void tearDown() throws Exception {
        root.delete();
    }

    @Test
    public void deserialiseQuietly_ShouldReturnNullIfFailsToDeserialise() throws Exception {
        Path userPath = usersDir.toPath().resolve("corrupt_user.json");
        userPath.toFile().createNewFile();

        Files.write(userPath, "abcdefghijklmnopqrstuvwxyz".getBytes(), StandardOpenOption.APPEND);

        assertThat(serialiser.deserialiseQuietly(userPath), equalTo(null));
    }

    @Test
    public void deserialiseQuietly_ShouldDeserialiseSuccessfully() throws IOException {
        User user = new User();
        user.setEmail("motherOfDragons@ons.gov.uk");
        user.setName("Daenerys Targaryen");
        user.setLastAdmin("admin@ons.gov.uk");
        user.setInactive(false);
        user.setTemporaryPassword(false);

        Path userPath = usersDir.toPath().resolve("motherOfDragonsons.gov.uk");
        userPath.toFile().createNewFile();

        Files.write(userPath, new Gson().toJson(user).getBytes(), StandardOpenOption.APPEND);

        assertThat(serialiser.deserialiseQuietly(userPath), equalTo(user));
    }

    @Test (expected = JsonSyntaxException.class)
    public void deserialise_ShouldProgegateExceptions() throws Exception {
        Path userPath = usersDir.toPath().resolve("corrupt_user.json");
        userPath.toFile().createNewFile();
        Files.write(userPath, "abcdefghijklmnopqrstuvwxyz".getBytes(), StandardOpenOption.APPEND);

        serialiser.deserialise(userPath);
    }

    @Test
    public void deserialise_ShouldDeserialiseSuccessfully() throws Exception {
        User user = new User();
        user.setEmail("motherOfDragons@ons.gov.uk");
        user.setName("Daenerys Targaryen");
        user.setLastAdmin("admin@ons.gov.uk");
        user.setInactive(false);
        user.setTemporaryPassword(false);

        Path userPath = usersDir.toPath().resolve("motherOfDragonsons.gov.uk");
        userPath.toFile().createNewFile();

        Files.write(userPath, new Gson().toJson(user).getBytes(), StandardOpenOption.APPEND);

        assertThat(serialiser.deserialise(userPath), equalTo(user));
    }
}
