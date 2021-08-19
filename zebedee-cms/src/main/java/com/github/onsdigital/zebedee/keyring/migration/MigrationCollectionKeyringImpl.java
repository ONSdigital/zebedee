package com.github.onsdigital.zebedee.keyring.migration;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.Set;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.warn;
import static java.text.MessageFormat.format;

/**
 * MigrationCollectionKeyringImpl serves 2 purposes:
 * <ul>
 *     <li>It uses a feature flag to determine which {@link CollectionKeyring} implementation to use (legacy or new central).</li>
 *     <li>If the central keyring feature is disabled keys are read from the legacy keyring but adds/removes are
 *     applied to both legacy and central keyrings. This enables us to migrate smoothly from the old keyring
 *     without losing any keys.</li>
 * </ul>
 */
public class MigrationCollectionKeyringImpl implements CollectionKeyring {

    static final String WRAPPED_ERR_FMT = "{0}: Migration enabled: {1}";
    static final String MIGRATE_ENABLED = "migration_enabled";
    static final String ACTION = "action";
    static String ADD_KEY = "add_key";
    static String REMOVE_KEY = "remove_key";

    static final String POPULATE_FROM_USER_ERR = "error while attempting to populate keyring from user";
    static final String GET_KEY_ERR = "error getting key from keyring";
    static final String REMOVE_KEY_ERR = "error removing key from keyring";
    static final String KEY_NULL_ERR = "get key returned null";
    static final String ADD_KEY_ERR = "error adding key to keyring";
    static final String ROLLBACK_FAILED_ERR = "rollback action was unsuccessful";
    static final String LIST_KEYS_ERR = "error listing keys on keyring";
    static final String UNLOCK_KEYRING_ERR = "error while attempting to unlock user kerying";

    private boolean migrationEnabled;
    private CollectionKeyring legacyCollectionKeyring;
    private CollectionKeyring collectionKeyring;

    /**
     * Construct a new instance of the Keyring.
     *
     * @param migrationEnabled if true uses the new central keyring implementation. Otherwise uses legacy keyring
     *                         implemenation for reads. (Writes/Deletes will be applied to both).
     * @param legacyCollectionKeyring    the legacy keyring implementation to use.
     * @param collectionKeyring   the new central keyring implementation to use.
     */
    public MigrationCollectionKeyringImpl(final boolean migrationEnabled,
                                          final CollectionKeyring legacyCollectionKeyring,
                                          final CollectionKeyring collectionKeyring) {
        this.migrationEnabled = migrationEnabled;
        this.legacyCollectionKeyring = legacyCollectionKeyring;
        this.collectionKeyring = collectionKeyring;
    }

    @Override
    public void cacheKeyring(User user) throws KeyringException {
        try {
            legacyCollectionKeyring.cacheKeyring(user);
            collectionKeyring.cacheKeyring(user);
        } catch (KeyringException ex) {
            throw wrappedKeyringException(ex, POPULATE_FROM_USER_ERR);
        }
    }

    @Override
    public SecretKey get(User user, Collection collection) throws KeyringException {
        try {
            return getKeyring().get(user, collection);
        } catch (KeyringException ex) {
            throw wrappedKeyringException(ex, GET_KEY_ERR);
        }
    }

    /**
     * Attempts to remove a key from both instances of the keyring. The central keyring is updated first.
     * <ul>
     *     <li>If centralKeyring.Remove is unsucessful the exception is thrown immediately and there no attempt to
     *     update the legacy keyring.</li>
     *     <li>
     *      If legacyKeyring.Remove is unsuccessful a rollback is attempted (re-adding the key to the central
     *      keyring) before returning the thrown exception.
     *     </li>
     *     <li>If the rollback fails the exception is returned</li>
     * </ul>
     *
     * @param user       the user performing the action.
     * @param collection the collection the the key belongs to.
     * @throws KeyringException
     */
    @Override
    public void remove(User user, Collection collection) throws KeyringException {
        // Get the key to delete in case a rollback is required
        SecretKey backUp = get(user, collection);
        if (backUp == null) {
            throw keyringException(KEY_NULL_ERR);
        }

        // try removing from the new keyring implementation first.
        removeFromKeyring(collectionKeyring, user, collection);

        // Remove from the legacy keyring.
        try {
            removeFromKeyring(legacyCollectionKeyring, user, collection);
        } catch (KeyringException ex) {

            // Remove failed so attempt rollback. If successful throw original exception otherwise throw the
            // failed rollback exception.
            Rollback rb = () -> collectionKeyring.add(user, collection, backUp);
            attemptRollback(rb, user, collection, REMOVE_KEY);

            throw ex;
        }
    }

    private void removeFromKeyring(CollectionKeyring instance, User user, Collection collection)
            throws KeyringException {
        try {
            instance.remove(user, collection);
        } catch (KeyringException ex) {
            error().data(MIGRATE_ENABLED, migrationEnabled)
                    .collectionID(collection)
                    .user(user.getEmail())
                    .exception(ex)
                    .log("error removing key from legacy kerying");

            throw wrappedKeyringException(ex, REMOVE_KEY_ERR);
        }
    }

    /**
     * Add a key to both instances of the keyring. The legacy keyring is updated first.
     * <ul>
     *     <li>If legacy keyring add is unsucessful an exception is thrown immediately and there no attempt to
     *     update the central  keyring.</li>
     *     <li>
     *      If central keyring add is unsuccessful a rollback is attempted (removing the newly added key from the legacy
     *      keyring) before returning the thrown exception.
     *     </li>
     *     <li>If the rollback fails the exception is returned</li>
     * </ul>
     *
     * @param user       the user performing the action.
     * @param collection the collection the the key belongs to.
     * @param key        the {@link SecretKey} to add to the keyring
     * @throws KeyringException
     */
    @Override
    public void add(User user, Collection collection, SecretKey key) throws KeyringException {
        // try adding the key to the legacy keyring
        addToKeyring(legacyCollectionKeyring, user, collection, key);

        // Try adding the key to the central keyring
        try {
            addToKeyring(collectionKeyring, user, collection, key);
        } catch (KeyringException ex) {

            // Add failed so attemp to rollback. If rollback fails throw rollback failed exception otherwise throw
            // the original exception
            Rollback rb = () -> legacyCollectionKeyring.remove(user, collection);
            attemptRollback(rb, user, collection, ADD_KEY);

            throw ex;
        }
    }

    private void addToKeyring(CollectionKeyring instance, User user, Collection collection, SecretKey key)
            throws KeyringException {
        try {
            instance.add(user, collection, key);
        } catch (KeyringException ex) {
            error().data(MIGRATE_ENABLED, migrationEnabled)
                    .user(user.getEmail())
                    .collectionID(collection)
                    .log("error adding key to keyring");

            throw wrappedKeyringException(ex, ADD_KEY_ERR);
        }
    }

    @Override
    public Set<String> list(User user) throws KeyringException {
        try {
            return getKeyring().list(user);
        } catch (KeyringException ex) {
            throw wrappedKeyringException(ex, LIST_KEYS_ERR);
        }
    }

    @Override
    public void unlock(User user, String password) throws KeyringException {
        try {
            // The central keyring does not need to be unlocked. While migrating we only need to unlock the legacy
            // keyring.
            legacyCollectionKeyring.unlock(user, password);
        } catch (KeyringException ex) {
            throw wrappedKeyringException(ex, UNLOCK_KEYRING_ERR);
        }
    }

    @Override
    public void assignTo(User src, User target, List<CollectionDescription> assignments) throws KeyringException {
        legacyCollectionKeyring.assignTo(src, target, assignments);
    }

    @Override
    public void assignTo(User src, User target, CollectionDescription... assignments) throws KeyringException {
        legacyCollectionKeyring.assignTo(src, target, assignments);
    }

    @Override
    public void revokeFrom(User target, List<CollectionDescription> removals) throws KeyringException {
        legacyCollectionKeyring.revokeFrom(target, removals);
    }

    @Override
    public void revokeFrom(User target, CollectionDescription... removals) throws KeyringException {
        legacyCollectionKeyring.revokeFrom(target, removals);
    }

    private CollectionKeyring getKeyring() {
        if (migrationEnabled) {
            return collectionKeyring;
        }

        return legacyCollectionKeyring;
    }

    /**
     * Attempt to rollback the keyring. Throws a new {@link KeyringException} if the rollback is unsuccessful.
     *
     * @param rollback   the {@link Rollback} to attempt.
     * @param user       the {@link User} executing this action.
     * @param collection the {@link Collection} the affected key belongs to.
     * @param action     the action being rolled back - add, remove etc.
     * @throws KeyringException failed to rollback the action.
     */
    private void attemptRollback(Rollback rollback, User user, Collection collection, String action)
            throws KeyringException {
        error().data(MIGRATE_ENABLED, migrationEnabled)
                .data(ACTION, action)
                .user(user.getEmail())
                .collectionID(collection)
                .log("attempting keyring action rollback");

        try {
            rollback.attempt();
        } catch (KeyringException ex) {
            error().data(MIGRATE_ENABLED, migrationEnabled)
                    .data(ACTION, action)
                    .user(user.getEmail())
                    .collectionID(collection)
                    .exception(ex)
                    .log("rollback keyring action unsuccessful");

            throw wrappedKeyringException(ex, ROLLBACK_FAILED_ERR);
        }

        warn().data(MIGRATE_ENABLED, migrationEnabled)
                .data(ACTION, action)
                .user(user.getEmail())
                .collectionID(collection)
                .log("rollback completed successfully");
    }

    KeyringException wrappedKeyringException(KeyringException cause, String msg) {
        return new KeyringException(format(WRAPPED_ERR_FMT, msg, migrationEnabled), cause);
    }

    KeyringException keyringException(String msg) {
        return new KeyringException(format(WRAPPED_ERR_FMT, msg, migrationEnabled));
    }
}
