package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.session.model.Session;

import java.io.IOException;
import java.util.concurrent.Callable;

@FunctionalInterface
public interface ZebedeeCollectionReaderSupplier {

    ZebedeeCollectionReader get(Zebedee zebedee, Collection collection, Session session)
            throws ZebedeeException, IOException;
}
