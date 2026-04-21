package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.UserDataPayload;
import com.github.onsdigital.dp.authorisation.permissions.PermissionChecker;
import com.github.onsdigital.dp.permissions.api.sdk.PermissionsClient;
import com.github.onsdigital.dp.permissions.api.sdk.exception.PolicyNotFoundException;
import com.github.onsdigital.dp.permissions.api.sdk.exception.PermissionsAPIException;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.permissions.model.Permissions;

import org.joda.time.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.warn;
import static java.text.MessageFormat.format;

public class PermissionsServiceImplementation implements PermissionsService {
    private static final String ADMIN_GROUP = "role-admin";
    private static final String UNSUPPORTED_ERROR = "Permissions API is enabled: {0} is no longer supported";

    private PermissionChecker permissionChecker;
    /** 
     * permissionsAPIClient is used for managing collection based permissions policies, which are currently only used for viewer teams.
     * This client uses the serviceAuthToken and so should only be used for background jobs, not for handling user requests where 
     * the user's permissions should be checked.
    */
    private PermissionsClient permissionAPIClient;

    private static final Duration DEFAULT_CACHE_UPDATE_INTERVAL = Duration.standardSeconds(60);
    private static final Duration DEFAULT_EXPIRY_CHECK_INTERVAL = Duration.standardSeconds(60);
    private static final Duration DEFAULT_MAX_CACHE_TIME = Duration.standardMinutes(5);

    public PermissionsServiceImplementation(PermissionsClient permissionAPIClient, String permissionsAPIHost) {
        this.permissionChecker = new PermissionChecker(permissionsAPIHost, DEFAULT_CACHE_UPDATE_INTERVAL, DEFAULT_EXPIRY_CHECK_INTERVAL, DEFAULT_MAX_CACHE_TIME);
        this.permissionAPIClient = permissionAPIClient;
    }

    public PermissionsServiceImplementation() {
        this.permissionChecker = new PermissionChecker("", DEFAULT_CACHE_UPDATE_INTERVAL, DEFAULT_EXPIRY_CHECK_INTERVAL, DEFAULT_MAX_CACHE_TIME);
    }

    @Override
    public boolean isAdministrator(Session session) throws IOException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "isAdministrator"));
    }

    @Override
    public boolean hasAdministrator() throws IOException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "hasAdministrator"));
    }

    @Override
    public void addAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "addAdministrator"));
    }

    @Override
    public void removeAdministrator(String email, Session session) throws IOException, UnauthorizedException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "removeAdministrator"));

    }

    @Override
    public boolean canEdit(Session session, CollectionType collectionType) throws IOException {
        boolean authorised = false;
        try {
            authorised = hasPermission(session, Permissions.LEGACY_EDIT, "", Optional.ofNullable(collectionType));
        } catch (Exception e){
            return false;
        }
        return authorised;   
    }

    @Override
    public boolean canEdit(Session session) throws IOException {
        return canEdit(session, null);
    }

    @Override
    public boolean canSelfApprove(Session session, CollectionType collectionType) throws IOException {
        boolean authorised = false;
        try {
            authorised = hasPermission(session, Permissions.LEGACY_SELF_APPROVE, "", Optional.ofNullable(collectionType));
        } catch (Exception e){
            return false;
        }
        return authorised;    
    }

    @Override
    public void addEditor(String email, Session session) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "addEditor"));
    }

    @Override
    public void removeEditor(String email, Session session) throws IOException, UnauthorizedException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "removeEditor"));
    }

    @Override
    public boolean canView(Session session, String collectionId, CollectionType collectionType) throws IOException {
        boolean authorised = false;
        try {
            authorised = hasPermission(session, Permissions.LEGACY_READ, collectionId, Optional.ofNullable(collectionType));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } 
        return authorised;
    }

    @Override
    public boolean canView(Session session, String collectionId) throws IOException {
        return canView(session, collectionId, null);
    }

    @Override
    public Set<String> listViewerTeams(Session session, String collectionId) throws IOException, UnauthorizedException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "listViewerTeams"));
    }

    @Override
    public void setViewerTeams(Session session, String collectionId, Set<String> collectionTeams) throws IOException, ZebedeeException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "setViewerTeams"));

    }

    @Override
    public PermissionDefinition userPermissions(String email, Session session) throws IOException, NotFoundException, UnauthorizedException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "userPermissions"));
    }

    @Override
    public PermissionDefinition userPermissions(Session session) throws IOException {
        throw new UnsupportedOperationException(format(UNSUPPORTED_ERROR, "userPermissions"));
    }

     /**
     * removePolicyForCollection removes a collection based permissions policy.
     * @param collectionId the ID of the collection to remove the permissions policy for.
     * @throws ZebedeeException if an error occurs while removing the permissions policy.
     */
    @Override
    public void removePolicyForCollection(String collectionId) throws ZebedeeException {
        info().collectionID(collectionId).log("removing permissions policy for collection");

        try {
            permissionAPIClient.deletePolicy(collectionId);
        } catch (PolicyNotFoundException e) {
            warn().collectionID(collectionId).log("no permissions policy found for collection id");
        } catch (IOException | com.github.onsdigital.dp.permissions.api.sdk.exception.BadRequestException | PermissionsAPIException e) {
            error().collectionID(collectionId).log("failed to remove permissions policy for collection id");
            throw new InternalServerError(format("failed to remove permissions policy for collection id: {0}", collectionId), e);
        }
    }

    private Boolean hasPermission(Session session, String permissionString, String collectionId, Optional<CollectionType> collectionType) throws Exception {
        HashMap<String, String> attributes = new HashMap<>();
        attributes.put("collection_id", collectionId);

        if (collectionType.isPresent()) {
            attributes.put("collection_type", collectionType.get().name());
        } else {
            attributes.put("collection_type", "");
        }
 
        UserDataPayload userDataPayload = new UserDataPayload(session.getId(), session.getEmail(), session.getGroups());

        return permissionChecker.hasPermission(userDataPayload, permissionString, attributes);
    }
}
