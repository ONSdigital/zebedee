package com.github.onsdigital.zebedee.collection.service;

import com.github.onsdigital.zebedee.collection.model.Collection;
import com.github.onsdigital.zebedee.collection.model.CollectionType;
import com.github.onsdigital.zebedee.collection.store.CollectionsStore;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;

import java.util.Date;
import java.util.List;

public class CollectionsServiceImpl implements CollectionsService{

    private CollectionsStore store;
    private PermissionsService permissionsService;

    public CollectionsServiceImpl(CollectionsStore store, PermissionsService permissionsService) {
        this.store = store;
        this.permissionsService = permissionsService;
    }

    @Override
    public List<Collection> listCollections(Session session) {
        return null;
    }

    @Override
    public Collection getCollection(Session session, String collectionId) {
        return null;
    }

    @Override
    public Collection createCollection(Session session, String name, CollectionType type, Date publishDate, String releaseUri) {
        return null;
    }

    @Override
    public void deleteCollection(Session session, String collectionId) {

    }
}
