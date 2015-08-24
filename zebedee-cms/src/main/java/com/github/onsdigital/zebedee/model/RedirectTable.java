package com.github.onsdigital.zebedee.model;


import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

/**
 * Redirect table interface
 *
 * Allows us to play with some different implementations
 */
public interface RedirectTable extends Iterable<String[]>{
    public String get(String uri);
    public void addRedirect(String redirectFrom, String redirectTo);
    public void removeRedirect(String redirectFrom, String redirectTo);

    public void merge(RedirectTable redirectTable);
    public void save(Path path) throws IOException;
    public void load(Path path) throws IOException;

    public boolean exists(String redirectFrom, String redirectTo);
}
