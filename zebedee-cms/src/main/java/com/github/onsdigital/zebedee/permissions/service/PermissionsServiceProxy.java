package com.github.onsdigital.zebedee.permissions.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;;

/**
 * A proxy class that sits in front of the 2 permissions service implementations (legacy & JWT). The class does not
 * perform any permisisons logic it determines which instance to invoke (legacy or JWT) based on the value of the
 * feature flag.
 */
public class PermissionsServiceProxy implements PermissionsService {

    private PermissionsService legacyPermissionsService;
    private PermissionsService jwtPermissionsService;
    private boolean jwtSessionsEnabled;

    public PermissionsServiceProxy(boolean jwtSessionsEnabled,
                                   PermissionsService legacyPermissionsService,
                                   PermissionsService jwtPermissionsService) {

        this.legacyPermissionsService = legacyPermissionsService;
        this.jwtPermissionsService = jwtPermissionsService;
        this.jwtSessionsEnabled = jwtSessionsEnabled;
    }

    @Override
    public boolean isPublisher(Session session) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.isPublisher(session);
        } 
        return legacyPermissionsService.isPublisher(session);
    }

    @Override
    public boolean isPublisher(String email) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.isPublisher(email);
        } 
        return legacyPermissionsService.isPublisher(email);
        
    }

    @Override
    public boolean isAdministrator(Session session) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.isAdministrator(session);
        } 
        return legacyPermissionsService.isAdministrator(session);
        
    }

    @Override
    public boolean isAdministrator(String email) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.isAdministrator(email);
        } 
        return legacyPermissionsService.isAdministrator(email);
    }

    @Override
    public List<User> getCollectionAccessMapping(Collection collection) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.getCollectionAccessMapping(collection);
        } 
        return legacyPermissionsService.getCollectionAccessMapping(collection);
        
    }
    
    @Override
    public boolean hasAdministrator() throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.hasAdministrator();
        } 
        return legacyPermissionsService.hasAdministrator();
        
    }

    @Override
    public void addAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        if (jwtSessionsEnabled) {
            jwtPermissionsService.addAdministrator(email,session);
        } 
        legacyPermissionsService.addAdministrator(email,session);
        
    }

    @Override
    public void removeAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        if (jwtSessionsEnabled) {
            jwtPermissionsService.removeAdministrator(email,session);
        } 
        legacyPermissionsService.removeAdministrator(email,session);
        
    }

    @Override
    public boolean canEdit(Session session) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.canEdit(session);
        } 
        return legacyPermissionsService.canEdit(session);
        
    }

    @Override
    public boolean canEdit(String email) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.canEdit(email);
        } 
        return legacyPermissionsService.canEdit(email);
        
    }

    @Override
    public boolean canEdit(User user) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.canEdit(user);
        } 
        return legacyPermissionsService.canEdit(user);
        
    }
    
    @Override
    public void addEditor(String email, Session session)
            throws IOException, UnauthorizedException, NotFoundException, BadRequestException {
                if (jwtSessionsEnabled) {
                    jwtPermissionsService.addEditor(email,session);
                } 
                legacyPermissionsService.addEditor(email,session);
        
    }

    @Override
    public void removeEditor(String email, Session session) throws IOException, UnauthorizedException {
        if (jwtSessionsEnabled) {
            jwtPermissionsService.removeEditor(email,session);
        } 
        legacyPermissionsService.removeEditor(email,session);
        
    }

    @Override
    public boolean canView(Session session, CollectionDescription collectionDescription) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.canView(session,collectionDescription);
        } 
        return legacyPermissionsService.canView(session,collectionDescription);
    }

    @Override
    public boolean canView(User user, CollectionDescription collectionDescription) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.canView(user,collectionDescription);
        } 
        return legacyPermissionsService.canView(user,collectionDescription);
    }

    @Override
    public boolean canView(String email, CollectionDescription collectionDescription) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.canView(email,collectionDescription);
        } 
        return legacyPermissionsService.canView(email,collectionDescription);
    }

    @Override
    public void addViewerTeam(CollectionDescription collectionDescription, Team team, Session session)
            throws IOException, ZebedeeException {
                if (jwtSessionsEnabled) {
                    jwtPermissionsService.addViewerTeam(collectionDescription,team,session);
                } 
                legacyPermissionsService.addViewerTeam(collectionDescription,team,session);
        
    }

    @Override
    public Set<Integer> listViewerTeams(CollectionDescription collectionDescription, Session session)
            throws IOException, UnauthorizedException {
                    if (jwtSessionsEnabled) {
                        return jwtPermissionsService.listViewerTeams(collectionDescription,session);
                    } 
                    return legacyPermissionsService.listViewerTeams(collectionDescription,session);
    }

    @Override
    public void removeViewerTeam(CollectionDescription collectionDescription, Team team, Session session)
            throws IOException, ZebedeeException {
                if (jwtSessionsEnabled) {
                    jwtPermissionsService.removeViewerTeam( collectionDescription,  team,  session);
                } 
                legacyPermissionsService.removeViewerTeam( collectionDescription,  team,  session);
        
    }

    @Override
    public PermissionDefinition userPermissions(String email, Session session)
            throws IOException, NotFoundException, UnauthorizedException {
                if (jwtSessionsEnabled) {
                    return jwtPermissionsService.userPermissions(email,session);
                } 
                return legacyPermissionsService.userPermissions(email,session);
}

    @Override
    public Set<String> listCollectionsAccessibleByTeam(Team t) throws IOException {
        if (jwtSessionsEnabled) {
            return jwtPermissionsService.listCollectionsAccessibleByTeam(t);
        } 
        return legacyPermissionsService.listCollectionsAccessibleByTeam(t);
    }


}