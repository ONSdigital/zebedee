package com.github.onsdigital.zebedee.util.versioning;

import com.github.onsdigital.zebedee.content.page.statistics.dataset.Version;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.session.model.Session;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public interface VersionsService {
    boolean isVersionedURI(String uri);

    Optional<String> getVersionNameFromURI(String uri);

    int getVersionNumberFromURI(String uri);

    Comparator<Version> versionComparator();

    boolean isVersionDir(File file);

    void verifyCollectionDatasets(ZebedeeReader cmsReader, Collection collection, CollectionReader reader,
                                  Session session) throws ZebedeeException, IOException, VersionNotFoundException;

    boolean isVersionOf(String parentURI, String s);

    List<String> getPreviousVersionsOf(String targetURI, Content content) throws IOException;
}
