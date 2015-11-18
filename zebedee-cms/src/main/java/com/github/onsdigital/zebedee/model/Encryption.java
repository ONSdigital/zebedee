package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.Team;
import com.github.onsdigital.zebedee.json.User;

import javax.crypto.SecretKey;
import java.io.IOException;

/**
 * Created by thomasridd on 18/11/15.
 */
public class Encryption {

    /**
     * Builds a user keyring from scratch
     *
     * @param session a session with publisher rights
     * @param user a user
     *
     * @throws IOException
     * @throws UnauthorizedException
     * @throws NotFoundException
     * @throws BadRequestException
     */
    public void buildUserKeyring(Zebedee zebedee, Session session, User user) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        if (!zebedee.permissions.canEdit(session.email)) throw new UnauthorizedException("User unauthorised");

        User sessionUser = Root.zebedee.users.get(session.email);

        // Remove all current keys
        user.keyring = user.keyring.emptyClone();

        // Walk through all current collections
        Collections.CollectionList collectionList = Root.zebedee.collections.list();
        for (Collection collection: collectionList) {
            // Add all keys for
            if (zebedee.permissions.canEdit(user.email)) {
                user.keyring.put(collection.description.id, sessionUser.keyring.get(collection.description.id));
            } else {
                // Distribute to team members
                // TODO: Whatever logic assigns users to teams to collections
            }
        }

        zebedee.users.updateKeyring(user);
    }

    /**
     * Distributes an encryption key
     *
     * @param session session for a user that possesses the key
     * @param collection a collection
     */
    public static void distributeCollectionKey(Zebedee zebedee, Session session, Collection collection) throws NotFoundException, BadRequestException, IOException, UnauthorizedException {
        User keyMaster = zebedee.users.get(session.email);

        SecretKey key = keyMaster.keyring.get(collection.description.id);

        // Distribute to all publishers
        for (User user: zebedee.users.list()) {
            if (zebedee.permissions.canEdit(user.email)) {
                // Add the key and save
                user.keyring.put(collection.description.id, key);
                zebedee.users.updateKeyring(user);
            }
        }

        // Distribute to team members
        // TODO: Whatever logic assigns users to teams to collections

    }
}
