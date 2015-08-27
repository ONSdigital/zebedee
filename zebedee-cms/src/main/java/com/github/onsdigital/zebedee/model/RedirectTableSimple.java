package com.github.onsdigital.zebedee.model;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by thomasridd on 14/08/15.
 */
public class RedirectTableSimple implements RedirectTable {
    Content content;
    Map<String, String> table = new ConcurrentHashMap<>(); // Concurrent is going to be necessary for save and add

    public RedirectTableSimple(Content content) {
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

        if (table.containsKey(redirectFrom) && table.get(redirectFrom).equalsIgnoreCase(redirectTo)) { return; }

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
    public void removeRedirect(String redirectFrom, String redirectTo) {
        // Clone and replace
        Map<String, String> newtable = new ConcurrentHashMap<>();
        for (String key: this.table.keySet()) {
            if (!(key.equalsIgnoreCase(redirectFrom) && table.get(key).equalsIgnoreCase(redirectTo)) ) {
                newtable.put(key, this.table.get(key));
            }
        }
        table = newtable;
    }

    @Override
    public void merge(RedirectTable redirectTable) {
        for (String[] fromTo: redirectTable) {
            addRedirect(fromTo[0], fromTo[1]);
        }
    }

    @Override
    public void save(Path path) throws IOException {
        // TODO quite a considerable amount of threadsafe making
        try (FileWriter stream = new FileWriter(path.toFile()); BufferedWriter out = new BufferedWriter(stream)) {
            for (String[] fromTo : this) {
                out.write(fromTo[0] + '\t' + fromTo[1]);
                out.newLine();
            }
        }
    }

    @Override
    public void load(Path path) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                // process the line.
                String[] fromTo = line.split("\t");
                addRedirect(fromTo[0], fromTo[1]);
            }
        }
    }

    @Override
    public boolean exists(String redirectFrom, String redirectTo) {
        return table.containsKey(redirectFrom);
    }

    @Override
    public Iterator<String[]> iterator() {
        Iterator<String[]> it = new Iterator<String[]>() {

            private Iterator<String> keyset = table.keySet().iterator();

            @Override
            public boolean hasNext() {
                return keyset.hasNext();
            }

            @Override
            public String[] next() {

                String key = keyset.next();


                String[] fromTo = new String[1];
                fromTo[0] = key;
                fromTo[1] = table.get(key);
                return fromTo;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return it;
    }
}
