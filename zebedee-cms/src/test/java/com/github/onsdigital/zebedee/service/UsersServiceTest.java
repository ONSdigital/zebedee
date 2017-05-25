package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.model.Permissions;
import com.github.onsdigital.zebedee.model.encryption.ApplicationKeys;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 */
public class UsersServiceTest {

    private static final String EMAIL = "test@ons.gov.uk";

    @Rule
    public TemporaryFolder root = new TemporaryFolder();

    @Mock
    private Collections collections;

    @Mock
    private Permissions permissions;

    @Mock
    private ApplicationKeys applicationKeys;

    @Mock
    private KeyringCache keyringCache;

    private Path usersPath;
    private UsersService service;
    private User user;

    private void createUser(User u) throws IOException {
        String filename = PathUtils.toFilename(StringUtils.lowerCase(StringUtils.trim(u.getEmail())));
        Path p = usersPath.resolve(filename + ".json");
        try (FileWriter fw = new FileWriter(p.toString())) {
            fw.write(new Gson().toJson(u));
            fw.flush();
        }
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        root.create();
        this.usersPath = root.newFolder("users").toPath();

        user = new User();
        user.setEmail(EMAIL);
        user.setName("JIM");
        user.setInactive(false);
        user.setTemporaryPassword(false);

        service = new UsersServiceImpl(usersPath, collections, permissions, applicationKeys, keyringCache);
    }

    @Test (expected = BadRequestException.class)
    public void getUserByEmail_ShouldThrowErrorForEmailNull() throws Exception {
        service.getUserByEmail(null);
    }

    @Test (expected = NotFoundException.class)
    public void getUserByEmail_ShouldThrowErrorIfUserDoesNotExist() throws Exception {
        service.getUserByEmail(EMAIL);
    }
}
