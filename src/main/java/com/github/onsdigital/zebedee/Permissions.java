package com.github.onsdigital.zebedee;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.AccessMapping;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Handles permissions mapping between users and {@link com.github.onsdigital.zebedee.Zebedee} functions.
 * Created by david on 12/03/2015.
 */
public class Permissions {
    private Path permissions;
    Timer timer;

    public Permissions(Path permissions) {
        this.permissions = permissions;
    }

    public boolean canEdit(String email) throws IOException {
        AccessMapping accessMapping = readAccessMapping();
        return canEdit(email, accessMapping);
    }

    public boolean canView(String email, String path) throws IOException {
        AccessMapping accessMapping = readAccessMapping();
        return canEdit(email, accessMapping) || canView(email, path, accessMapping);
    }

    public void addEditor(String email) throws IOException {
        AccessMapping accessMapping = readAccessMapping();
        accessMapping.digitalPublishingTeam.add(email);
        writeAccessMapping(accessMapping);
    }

    public void removeEditor(String email) throws IOException {
        AccessMapping accessMapping = readAccessMapping();
        accessMapping.digitalPublishingTeam.remove(email);
        writeAccessMapping(accessMapping);
    }

    public void addViewer(String email, String path) throws IOException {
        AccessMapping accessMapping = readAccessMapping();
        Set viewers = accessMapping.paths.get(path);
        if (viewers == null) {
            viewers = new HashSet<>();
        }
        viewers.add(email);
        accessMapping.paths.put(path, viewers);
        writeAccessMapping(accessMapping);
    }

    public void removeViewer(String email, String path) throws IOException {
        AccessMapping accessMapping = readAccessMapping();

        // Check to see if the requested path matches (or is a sub-path of) any mapping:
        for (Map.Entry<String, Set<String>> mapping : accessMapping.paths.entrySet()) {
            boolean isSubPath = StringUtils.startsWithIgnoreCase(mapping.getKey(), path);
            boolean emailMatches = mapping.getValue().contains(email);
            if (isSubPath && emailMatches) {
                mapping.getValue().remove(email);
            }
        }
        writeAccessMapping(accessMapping);
    }

    private boolean canEdit(String email, AccessMapping accessMapping) throws IOException {
        Set<String> digitalPublishingTeam = accessMapping.digitalPublishingTeam;
        return digitalPublishingTeam.contains(email);
    }

    private boolean canView(String email, String path, AccessMapping accessMapping) {

        // Check to see if the requested path matches (or is a sub-path of) any mapping:
        for (Map.Entry<String, Set<String>> mapping : accessMapping.paths.entrySet()) {
            boolean isSubPath = StringUtils.startsWithIgnoreCase(path, mapping.getKey());
            boolean emailMatches = mapping.getValue().contains(email);
            if (isSubPath && emailMatches) {
                return true;
            }
        }

        // No match found:
        return false;
    }

    private AccessMapping readAccessMapping() throws IOException {
        Path path = permissions.resolve("accessMapping.json");
        System.out.println(path);

        // Read the configuration
        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                return Serialiser.deserialise(input, AccessMapping.class);
            }
        }

        // Or generate a new one:
        System.out.println("Generating new access mapping configuration.");
        AccessMapping accessMapping = new AccessMapping();
        accessMapping.digitalPublishingTeam = new HashSet<>();
        accessMapping.paths = new HashMap<>();
        try (OutputStream output = Files.newOutputStream(path)) {
            Serialiser.serialise(output, accessMapping);
        }
        return accessMapping;
    }

    private void writeAccessMapping(AccessMapping accessMapping) throws IOException {
        Path path = permissions.resolve("accessMapping.json");
        System.out.println(path);

        try (OutputStream output = Files.newOutputStream(path)) {
            Serialiser.serialise(output, accessMapping);
        }
    }


}
