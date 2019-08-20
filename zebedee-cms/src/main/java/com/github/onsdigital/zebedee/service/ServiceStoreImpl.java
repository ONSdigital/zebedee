package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.util.serialiser.JSONSerialiser;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServiceStoreImpl implements ServiceStore {

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
            String sessionFileName = id + JSON_EXTENSION;
            result = rootLocation.resolve(sessionFileName);
        }
        return result;
    }
}
