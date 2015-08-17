package com.github.onsdigital.zebedee.model;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Redirect table interface
 *
 * Allows us to play with some different implementations
 */
public interface RedirectTable {
    public String get(String uri);
    public void addRedirect(String redirectFrom, String redirectTo);
    public void removeRedirect(String redirectFrom);
    public void save(Path path) throws IOException;
    public void load(Path path) throws IOException;
}
