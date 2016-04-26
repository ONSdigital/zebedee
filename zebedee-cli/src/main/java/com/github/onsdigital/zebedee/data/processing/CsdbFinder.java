package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class CsdbFinder extends SimpleFileVisitor<Path> {

    List<Path> files = new ArrayList<>();
    List<String> uris = new ArrayList<>();
    Path root;

    public List<Path> find(Path root) {
        this.root = root;

        try {
            Files.walkFileTree(root, this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this.files;
    }


    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attr) throws IOException {
        // Get the uri
        String uri = "/" + root.relativize(path).toString();

        // Check json files in timeseries directories
        if (uri.endsWith(".csdb") && uri.toString().contains("/datasets/")) {

            if (VersionedContentItem.isVersionedUri(uri))
                return FileVisitResult.CONTINUE;

            //uri = uri.substring(0, uri.length() - "/data.json".length());

            //Log.print("Adding file with uri: %s and path %s", uri, path.toString());
            this.files.add(path);
            this.uris.add(uri);
        }
        return FileVisitResult.CONTINUE;
    }
}
