package com.github.onsdigital.zebedee.model.publishing.verify;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.CollectionReader;

public interface HashVerifier {

    void verifyTransactionContent(Collection collection, CollectionReader reader) throws HashVerificationException;
}
