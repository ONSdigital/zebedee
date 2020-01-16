package com.github.onsdigital.zebedee.model.publishing.verify;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.CollectionReader;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public interface HashVerifier {

    void verifyTransactionContent(Collection collection, CollectionReader reader) throws IOException,
            InterruptedException, ExecutionException;
}
