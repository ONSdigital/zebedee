package com.github.onsdigital.zebedee.util.versioning;

import java.io.File;
import java.util.Optional;

public interface VersionsService {
    boolean isVersionedURI(String uri);

    Optional<String> getVersionNameFromURI(String uri);

    boolean isVersionDir(File file);
}
