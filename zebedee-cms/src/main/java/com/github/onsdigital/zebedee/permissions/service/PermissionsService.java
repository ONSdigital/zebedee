package com.github.onsdigital.zebedee.permissions.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.user.model.User;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Interface definding the api of a permissions service.
 */
public interface PermissionsService {

    /**
     * Return true if the {@link User} is a publisher, false otherwise.
     *
     * @param session {@link Session} to get the user details from.
     * @return true if the {@link User} is a publisher, false otherwise.
     * @throws IOException unexpected error checking the user permissions.
     */
    boolean isPublisher(Session session) throws IOException;

    /**
     * Return true if the {@link User} is a publisher, false otherwise.
     *
     * @param email the email of the user to check.
     * @return true if the {@link User} is a publisher, false otherwise.
     * @throws IOException unexpected error checking the user permissions.
     */
    boolean isPublisher(String email) throws IOException;

    /**
     * Return true if the {@link User} is an Admin, false otherwise.
     *
     * @param session {@link Session} to get the user details from.
     * @return true if the user is an admin, false otherwise.
     * @throws IOException unexpected error checking the user permissions.
     */
    boolean isAdministrator(Session session) throws IOException;

    /**
     * Return true if the {@link User} is an Admin, false otherwise.
     *
     * @param email the email of the user to check.
     * @return true if the user is an admin, false otherwise.
     * @throws IOException unexpected error checking the user permissions.
     */
    boolean isAdministrator(String email) throws IOException;

    /**
     * Return a list of {@link User} who have access to the specified collection.
     *
     * @param collection the collection to check users against.
     * @return {@link List} of {@link User} who have access to the collection.
     * @throws IOException unexpected error checking the users against the collection.
     */
    List<User> getCollectionAccessMapping(Collection collection) throws IOException;

    /**
     * @return true if an Admin user exists, false otherwise.
     * @throws IOException unexpected error accessing users.
     */
    boolean hasAdministrator() throws IOException;

    /**
     * Grant a {@link User} admin permissions.
     *
     * @param email   the email of the user to grant the permission to.
     * @param session the {@link Session} of the user granting the permission.
     * @throws IOException           unexpected error granting the permission.
     * @throws UnauthorizedException user does not have the required permissions to grant admin permissions.
     */
    void addAdministrator(String email, Session session) throws IOException, UnauthorizedException;

    /**
     * Revoke admin permission from a user.
     *
     * @param email   the email of the user to remove the permission from.
     * @param session the {@link Session} of the user revoking the permission.
     * @throws IOException           unexpected error revoking the permission.
     * @throws UnauthorizedException user revoking the permission does not have the required permissions.
     */
    void removeAdministrator(String email, Session session) throws IOException, UnauthorizedException;

    /**
     * Check if the {@link User} has permissions to edit content.
     *
     * @param session the {@link Session} of the user to check.
     * @return true if the user can edit content, false otherwise.
     * @throws IOException unexpected error while checking permissions.
     */
    boolean canEdit(Session session) throws IOException;

    /**
     * Check if the {@link User} has permissions to edit content.
     *
     * @param email the email of the user to check.
     * @return true if the user can edit content, false otherwise.
     * @throws IOException unexpected error while checking permissions.
     */
    boolean canEdit(String email) throws IOException;

    /**
     * Check if the {@link User} has permissions to edit content.
     *
     * @param session               the {@link Session} of the user to check.
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to check.
     * @return true if the user has edit permissions, false otherwise.
     * @throws IOException unexpected error while checking permissions.
     */
    boolean canEdit(Session session, CollectionDescription collectionDescription) throws IOException;

    /**
     * Check if the {@link User} has permissions to edit content.
     *
     * @param user                  the {@link User} to check.
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to check.
     * @return true if the user has edit permissions, false otherwise.
     * @throws IOException unexpected error while checking permissions.
     */
    boolean canEdit(User user, CollectionDescription collectionDescription) throws IOException;

    /**
     * Check if the {@link User} has permissions to edit content.
     *
     * @param email                 the email of the user to check.
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to check.
     * @return true if the user has edit permissions, false otherwise.
     * @throws IOException unexpected error while checking permissions.
     */
    boolean canEdit(String email, CollectionDescription collectionDescription) throws IOException;

    /**
     * Grant editor permission to a user.
     *
     * @param email   the email of the user to grant the permission to.
     * @param session the {@link Session} of the {@link User} granting the permissison.
     * @throws IOException           unexpected error while granting the permission.
     * @throws UnauthorizedException the user granting the permission is not authorised to do so.
     * @throws NotFoundException     unexpected error while granting the permission.
     * @throws BadRequestException   unexpected error while granting the permission.
     */
    void addEditor(String email, Session session) throws IOException, UnauthorizedException, NotFoundException, BadRequestException;

    /**
     * Remoke editor permission from a user.
     *
     * @param email   the email of the user to revoke the permission from.
     * @param session the {@link Session} of the {@link User} revoking the permissison.
     * @throws IOException           unexpected error while granting the permission.
     * @throws UnauthorizedException the user revoking the permission is not authorised to do so.
     * @throws NotFoundException     unexpected error while granting the permission.
     * @throws BadRequestException   unexpected error while granting the permission.
     */
    void removeEditor(String email, Session session) throws IOException, UnauthorizedException;

    /**
     * Check if a {@link User} can view unpublished content.
     *
     * @param session               the {@link Session} to get the user details from.
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to check.
     * @return true of the user has view permission for the content, false otherwise.
     * @throws IOException unexpected error while checking permissions.
     */
    boolean canView(Session session, CollectionDescription collectionDescription) throws IOException;

    /**
     * Check if a {@link User} can view unpublished content.
     *
     * @param user                  the {@link User} to check.
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to check.
     * @return true of the user has view permission for the content, false otherwise.
     * @throws IOException unexpected error while checking permissions.
     */
    boolean canView(User user, CollectionDescription collectionDescription) throws IOException;

    /**
     * Check if a {@link User} can view unpublished content.
     *
     * @param email                 the email of the user to check.
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to check.
     * @return true of the user has view permission for the content, false otherwise.
     * @throws IOException unexpected error while checking permissions.
     */
    boolean canView(String email, CollectionDescription collectionDescription) throws IOException;

    /**
     * Grant view permissions to a {@link Team}.
     *
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} in question.
     * @param team                  the {@link Team} to grant view permission to.
     * @param session               the {@link Session} of the user granting the permission.
     * @throws IOException      unexpected error while checking permissions.
     * @throws ZebedeeException unexpected error while checking permissions.
     */
    void addViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException, ZebedeeException;

    /**
     * Returns a {@link List} of {@link Team}s that have viewer permissions on the specified collection.
     *
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to get the viewer
     *                              teams for.
     * @param session               the {@link Session} of the {@link User} requesting this information.
     * @return Returns a {@link List} of {@link Team}s that have viewer permissions on the specified collection.
     * @throws IOException           unexpected error while checking permissions.
     * @throws UnauthorizedException unexpected error while checking permissions.
     */
    Set<Integer> listViewerTeams(CollectionDescription collectionDescription, Session session) throws IOException, UnauthorizedException;

    /**
     * Revoke view permission from a {@link Team} for the specified {@link Collection}.
     *
     * @param collectionDescription the {@link CollectionDescription} of the {@link Collection} to remove the team.
     * @param team                  the {@link Team} to remove.
     * @param session               the {@link Session} of the user revoking view permission.
     * @throws IOException      unexpected error while revoking permissions.
     * @throws ZebedeeException unexpected error while revoking permissions.
     */
    void removeViewerTeam(CollectionDescription collectionDescription, Team team, Session session) throws IOException, ZebedeeException;

    /**
     * Return {@link PermissionDefinition} for the specified {@link User}.
     *
     * @param email   the email of the user to get the {@link PermissionDefinition} for.
     * @param session the {@link Session} of the user requesting the {@link PermissionDefinition}.
     * @return Return {@link PermissionDefinition} for the specified {@link User}.
     * @throws IOException           unexpected error while getting the user {@link PermissionDefinition}.
     * @throws NotFoundException     user with the specified email was not found.
     * @throws UnauthorizedException the requesting user does not have the required permissions.
     */
    PermissionDefinition userPermissions(String email, Session session) throws IOException, NotFoundException, UnauthorizedException;
}
