package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionOwner;
import com.github.onsdigital.zebedee.session.model.Session;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by dave on 31/05/2017.
 */
public interface PermissionsService {

    boolean isPublisher(Session session) throws IOException;

    boolean isPublisher(String email) throws IOException;

    boolean isAdministrator(Session session) throws IOException;

    boolean isAdministrator(String email) throws IOException;

    List<User> getCollectionAccessMapping(Collection collection) throws IOException;

    boolean isDataVisPublisher(String email, AccessMapping accessMapping) throws IOException;

    boolean hasAdministrator() throws IOException;

    void addAdministrator(String email, Session session) throws IOException, UnauthorizedException;

    void removeAdministrator(String email, Session session) throws IOException, UnauthorizedException;

    boolean canEdit(Session session) throws IOException;

    boolean canEdit(String email) throws IOException;

    boolean canEdit(Session session, CollectionDescription collectionDescription) throws IOException;

    boolean canEdit(User user, CollectionDescription collectionDescription) throws IOException;

    boolean canEdit(String email, CollectionDescription collectionDescription) throws IOException;

    void addEditor(String email, Session session) throws IOException, UnauthorizedException, NotFoundException, BadRequestException;

    void removeEditor(String email, Session session) throws IOException, UnauthorizedException;

    boolean canView(Session session, CollectionDescription collectionDescription) throws IOException;

    boolean canView(User user, CollectionDescription collectionDescription) throws IOException;

    boolean canView(String email, CollectionDescription collectionDescription) throws IOException;

    void addViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException, ZebedeeException;

    Set<Integer> listViewerTeams(CollectionDescription collectionDescription, Session session) throws IOException, UnauthorizedException;

    void removeViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException, ZebedeeException;

    PermissionDefinition userPermissions(String email, Session session) throws IOException, NotFoundException, UnauthorizedException;

    void addDataVisualisationPublisher(String email, Session session) throws ZebedeeException;

    void removeDataVisualisationPublisher(String email, Session session) throws IOException, UnauthorizedException;

    CollectionOwner getUserCollectionGroup(Session session) throws IOException;

    CollectionOwner getUserCollectionGroup(String email) throws IOException;

    CollectionOwner getUserCollectionGroup(String email, AccessMapping accessMapping) throws IOException;
}
