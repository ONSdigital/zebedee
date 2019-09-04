package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.util.serialiser.JSONSerialiser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.warn;

/**
 * File based service account store implementation.
 */
public class ServiceStoreImpl implements ServiceStore {

    private final Path serviceRoot;

    private final JSONSerialiser<ServiceAccount> jsonSerialise;

    /**
     * Construct a new ServiceStoreImpl instance.
     *
     * @param serviceRoot the file path of the service account root directory.
     */
    public ServiceStoreImpl(Path serviceRoot) {
        this.serviceRoot = serviceRoot;
        this.jsonSerialise = new JSONSerialiser<>(ServiceAccount.class);
    }

    /**
     * Construct a new ServiceStoreImpl instance.
     *
     * @param serviceRoot   the file path of the service account root directory.
     * @param jsonSerialise the {@link JSONSerialiser} implementation to use.
     */
    public ServiceStoreImpl(Path serviceRoot, JSONSerialiser<ServiceAccount> jsonSerialise) {
        this.serviceRoot = serviceRoot;
        this.jsonSerialise = jsonSerialise;
    }

    @Override
    public ServiceAccount get(String token) throws IOException {
        Path filePath = null;
        ServiceAccount account = null;

        if (ServiceTokenUtils.isValidServiceToken(token)) {
            filePath = getServiceAccountPath(token);
            account = getServiceAccount(filePath);
        }
        return account;
    }

    private ServiceAccount getServiceAccount(Path filePath) throws IOException {
        ServiceAccount account = null;

        if (filePath == null) {
            warn().log("error getting service account file path null");
        } else if (!Files.exists(filePath)) {
            warn().log("error getting service account file does not exist");
        } else {
            try (InputStream input = Files.newInputStream(filePath)) {
                account = jsonSerialise.deserialiseQuietly(input, filePath);
            } catch (Exception ex) {
                throw new IOException("error deserialising service account json", ex);
            }
        }
        return account;
    }

    @Override
    public ServiceAccount store(String token, InputStream service) throws IOException {
        Path path = getServiceAccountPath(token);

        if (Files.exists(path)) {
            throw new FileAlreadyExistsException("The service token already exists : " + path);
        }

        ServiceAccount serviceAccount = jsonSerialise.deserialiseQuietly(service, path);
        jsonSerialise.serialise(path, serviceAccount);
        return serviceAccount;
    }

    private Path getServiceAccountPath(String token) throws IOException {
        try {
            return ServiceTokenUtils.getServiceAccountPath(serviceRoot, token);
        } catch (IllegalArgumentException ex) {
            throw new IOException("error getting service account path for token", ex);
        } catch (Exception ex) {
            error().exception(ex).log("error getting service account file path");
            throw ex;
        }
    }
}
