package com.github.onsdigital.zebedee.service;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.util.serialiser.JSONSerialiser;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.warn;

public class ServiceStoreImpl implements ServiceStore {

    private static final Pattern VALID_FILENAME_REGEX = Pattern.compile("^\\w+$");

    private final Path rootLocation;

    private final JSONSerialiser<ServiceAccount> jsonSerialise;

    private final static String JSON_EXTENSION = ".json";

    public ServiceStoreImpl(Path rootLocation) {
        this.rootLocation = rootLocation;
        this.jsonSerialise = new JSONSerialiser<>(ServiceAccount.class);
    }

    @Override
    public ServiceAccount get(String id) throws IOException {
        ServiceAccount service = null;
        final Path path = getPath(id);
        if (Files.exists(getPath(id))) {
            try (InputStream input = Files.newInputStream(path)) {
                service = jsonSerialise.deserialiseQuietly(input, path);
            }
        }
        return service;
    }

    @Override
    public ServiceAccount store(String token, InputStream service) throws IOException {
        final Path path = rootLocation.resolve(token + JSON_EXTENSION);
        if (!Files.exists(path)) {
            ServiceAccount object = jsonSerialise.deserialiseQuietly(service, path);
            jsonSerialise.serialise(path, object);
            return object;
        }

        throw new FileAlreadyExistsException("The service token already exists : " + path);

    }

    private Path getPath(String id) {
        Path result = null;
        if (StringUtils.isNotBlank(id)) {
            validateToken(id);
            String sessionFileName = id + JSON_EXTENSION;
            result = rootLocation.resolve(sessionFileName);
        }
        return result;
    }

    private void validateToken(String token) {
        if (!VALID_FILENAME_REGEX.matcher(token).matches()) {
            warn().data("token", token).log("invalid service token value");
            throw new RuntimeException("invalid service token value: tokens must match alphanumeric with underscores");
        }
    }
}
