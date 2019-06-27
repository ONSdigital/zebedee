package com.github.onsdigital.zebedee.permissions.cmd;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.session.model.Session;

interface CollectionPermissionsService {

    boolean hasEdit(Session session) throws PermissionsException;

    boolean hasView(Session session, CollectionDescription description) throws PermissionsException;
}
