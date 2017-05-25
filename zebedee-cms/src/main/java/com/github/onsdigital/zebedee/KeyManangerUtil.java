package com.github.onsdigital.zebedee;

import com.github.davidcarboni.cryptolite.Keys;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.KeyManager;

import javax.crypto.SecretKey;
import java.io.IOException;

/**
 * Created by dave on 19/05/2017.
 */
public class KeyManangerUtil {

    public void assignKeyToUser(Zebedee zebedee, User user, String keyIdentifier, SecretKey key)
            throws IOException {
        KeyManager.assignKeyToUser(zebedee, user, keyIdentifier, Keys.newSecretKey());
    }

    public void distributeCollectionKey(Zebedee zebedee, Session session, Collection collection,
                                        boolean isNewCollection) throws IOException {
        KeyManager.distributeCollectionKey(zebedee, session, collection, true);
    }
}
