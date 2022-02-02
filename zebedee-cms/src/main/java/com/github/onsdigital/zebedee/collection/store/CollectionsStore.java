package com.github.onsdigital.zebedee.collection.store;

import com.github.onsdigital.zebedee.collection.model.Collection;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;

import java.io.IOException;
import java.util.Set;

public interface CollectionsStore {

    public Set<Collection> list() throws IOException;

    public Collection get(String collectionId) throws IOException, NotFoundException;

    public boolean exists(String collectionId);

    public void save(Collection collection) throws IOException, NotFoundException;

    public void delete(String collectionId) throws NotFoundException, IOException;
}
