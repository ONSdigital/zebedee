package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.*;


public class CompositeContentReader implements ContentReader {

    List<ContentReader> readers = new ArrayList<>();

    public CompositeContentReader(ContentReader reader) {
        add(reader);
    }

    public CompositeContentReader(ContentReader primaryReader, ContentReader secondaryReader) {
        add(primaryReader);
        add(secondaryReader);
    }

    public void add(ContentReader reader) {
        readers.add(reader);
    }

    @Override
    public Page getContent(String path) throws ZebedeeException, IOException {
        Iterator<ContentReader> iterator = readers.iterator();
        while (iterator.hasNext()) {
            ContentReader reader = iterator.next();
            try {
                return reader.getContent(path);
            } catch (ZebedeeException | IOException e) {
                if (!iterator.hasNext()) {
                    throw e;
                }
            }
        }
        return null; // should never happen
    }


    @Override
    public Page getLatestContent(String path) throws ZebedeeException, IOException {
        Iterator<ContentReader> iterator = readers.iterator();
        while (iterator.hasNext()) {
            ContentReader reader = iterator.next();
            try {
                return reader.getLatestContent(path);
            } catch (ZebedeeException | IOException e) {
                if (!iterator.hasNext()) {
                    throw e;
                }
            }
        }
        return null; // should never happen
    }

    @Override
    public Resource getResource(String path) throws ZebedeeException, IOException {
        Iterator<ContentReader> iterator = readers.iterator();
        while (iterator.hasNext()) {
            ContentReader reader = iterator.next();
            try {
                return reader.getResource(path);
            } catch (ZebedeeException | IOException e) {
                if (!iterator.hasNext()) {
                    throw e;
                }
            }
        }
        return null; // should never happen
    }

    @Override
    public long getContentLength(String path) throws ZebedeeException, IOException {
        Iterator<ContentReader> iterator = readers.iterator();
        while (iterator.hasNext()) {
            ContentReader reader = iterator.next();
            try {
                return reader.getContentLength(path);
            } catch (ZebedeeException | IOException e) {
                if (!iterator.hasNext()) {
                    throw e;
                }
            }
        }
        return 0; // should never happen
    }

    @Override
    public Map<URI, ContentNode> getChildren(String path) throws ZebedeeException, IOException {
        Iterator<ContentReader> iterator = readers.iterator();
        while (iterator.hasNext()) {
            ContentReader reader = iterator.next();
            try {
                return reader.getChildren(path);
            } catch (ZebedeeException | IOException e) {
                if (!iterator.hasNext()) {
                    throw e;
                }
            }
        }
        return null; // should never happen
    }

    @Override
    public DirectoryStream<Path> getDirectoryStream(String path) throws BadRequestException, IOException {
        Iterator<ContentReader> iterator = readers.iterator();
        while (iterator.hasNext()) {
            ContentReader reader = iterator.next();
            try {
                return reader.getDirectoryStream(path);
            } catch (ZebedeeException | IOException e) {
                if (!iterator.hasNext()) {
                    throw e;
                }
            }
        }
        return null; // should never happen
    }

    @Override
    public DirectoryStream<Path> getDirectoryStream(String path, String filter) throws BadRequestException, IOException {
        Iterator<ContentReader> iterator = readers.iterator();
        while (iterator.hasNext()) {
            ContentReader reader = iterator.next();
            try {
                return reader.getDirectoryStream(path, filter);
            } catch (ZebedeeException | IOException e) {
                if (!iterator.hasNext()) {
                    throw e;
                }
            }
        }
        return null; // should never happen
    }

    @Override
    public Map<URI, ContentNode> getParents(String path) throws ZebedeeException, IOException {
        Iterator<ContentReader> iterator = readers.iterator();
        while (iterator.hasNext()) {
            ContentReader reader = iterator.next();
            try {
                return reader.getParents(path);
            } catch (ZebedeeException | IOException e) {
                if (!iterator.hasNext()) {
                    throw e;
                }
            }
        }
        return null; // should never happen
    }

    @Override
    public Path getRootFolder() {
        Iterator<ContentReader> iterator = readers.iterator();
        while (iterator.hasNext()) {
            ContentReader reader = iterator.next();
            return reader.getRootFolder();
        }
        return null; // should never happen
    }

    @Override
    public ContentLanguage getLanguage() {
        Iterator<ContentReader> iterator = readers.iterator();
        while (iterator.hasNext()) {
            ContentReader reader = iterator.next();
            return reader.getLanguage();
        }
        return null; // should never happen
    }

    @Override
    public void setLanguage(ContentLanguage language) {
        Iterator<ContentReader> iterator = readers.iterator();
        while (iterator.hasNext()) {
            ContentReader reader = iterator.next();
            reader.setLanguage(language);
        }
    }

    @Override
    public List<String> listUris() {
        Iterator<ContentReader> iterator = readers.iterator();
        Set<String> uris = new HashSet<>();

        while (iterator.hasNext()) {
            ContentReader reader = iterator.next();
            uris.addAll(reader.listUris());
        }
        return new ArrayList<>(uris);
    }

    @Override
    public List<Path> listTimeSeriesDirectories() {
        Iterator<ContentReader> iterator = readers.iterator();
        Set<Path> uris = new HashSet<>();

        while (iterator.hasNext()) {
            ContentReader reader = iterator.next();
            uris.addAll(reader.listTimeSeriesDirectories());
        }
        return new ArrayList<>(uris);
    }
}
