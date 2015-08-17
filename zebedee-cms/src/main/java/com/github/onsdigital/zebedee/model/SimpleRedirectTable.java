package com.github.onsdigital.zebedee.model;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by thomasridd on 14/08/15.
 */
public class SimpleRedirectTable implements RedirectTable {
    Content content;
    Map<String, String> table = new ConcurrentHashMap<>(); // Concurrent is going to be necessary for save and add

    public SimpleRedirectTable(Content content) {
        this.content = content;
    }

    @Override
    public String get(String uri) {
        if (content.exists(uri, false)) {
            return uri;
        } else if (this.table.containsKey(uri) && content.exists(this.table.get(uri), false) ) {
            return this.table.get(uri);
        } else {
            return null;
        }
    }

    @Override
    public void addRedirect(String redirectFrom, String redirectTo) {

        Map<String, String> newtable = new ConcurrentHashMap<>();
        for (String key: this.table.keySet()) {
            String value = this.table.get(key);
            if (value.equalsIgnoreCase(redirectFrom)) {
                newtable.put(key, redirectTo);
            } else {
                newtable.put(key, value);
            }
            newtable.put(redirectFrom, redirectTo);
        }
        this.table = newtable;
    }

    @Override
    public void removeRedirect(String redirectFrom) {
        Map<String, String> newtable = new ConcurrentHashMap<>();
        for (String key: this.table.keySet()) {
            if (!key.equalsIgnoreCase(redirectFrom)) {
                newtable.put(key, this.table.get(key));
            }
        }
    }

    @Override
    public void save(Path path) throws IOException {

    }

    @Override
    public void load(Path path) throws IOException {

    }
}
