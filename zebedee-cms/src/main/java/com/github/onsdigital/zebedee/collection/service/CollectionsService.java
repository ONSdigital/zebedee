package com.github.onsdigital.zebedee.collection.service;

import com.github.onsdigital.zebedee.collection.model.Collection;
import com.github.onsdigital.zebedee.collection.model.CollectionType;
import com.github.onsdigital.zebedee.session.model.Session;

import java.util.Date;
import java.util.List;

public interface CollectionsService {

    /*
    TODO: Do we need to add auth at this level? If so then we need to pass the session. Doing so may cause issues for the
          automated processes though so we might have to have unauthenticated versions as well. More analysis needed.
     */

    public List<Collection> listCollections(Session session);

    public Collection getCollection(Session session, String collectionId);

    public Collection createCollection(Session session, String name, CollectionType type, Date publishDate, String releaseUri);

    public void deleteCollection(Session session, String collectionId);
}
