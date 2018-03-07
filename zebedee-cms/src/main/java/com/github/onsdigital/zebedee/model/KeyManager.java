package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;

/**
 * Created by thomasridd on 18/11/15.
 */
public class KeyManager {

    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();
    private static ServiceSupplier<Collections> collectionsServiceSupplier = () -> Root.zebedee.getCollections();

    static final ExecutorService executorService = Executors.newFixedThreadPool(25);

    public static void setCollectionsServiceSupplier(ServiceSupplier<Collections> supplier) {
        collectionsServiceSupplier = supplier;
    }

    /**
     * Distribute the collection key to users that should have it.
     *
     * @param zebedee         the {@link Zebedee} instance to use.
     * @param session         the users session.
     * @param collection      the {@link Collection} the key belongs to.
     * @param isNewCollection true if the collection is new, false otherwise.
     * @throws IOException
     */
    public static synchronized void distributeCollectionKey(Zebedee zebedee, Session session, Collection collection,
                                                            boolean isNewCollection) throws IOException {
        SecretKey key = zebedee.getKeyringCache()
                .get(session)
                .get(collection.getDescription().getId());


        List<User> keyRecipients = nullSafeList(zebedee
                .getPermissionsService()
                .getCollectionAccessMapping(collection));

        List<User> keyRevoked = new ArrayList<>();
        if (!isNewCollection) {
            keyRevoked = zebedee.getUsersService()
                    .list()
                    .stream()
                    .filter((user -> !keyRecipients.contains(user)))
                    .collect(Collectors.toList());
        }

        for (User removedUser : keyRevoked) {
            removeKeyFromUser(zebedee, removedUser, collection.getDescription().getId());
        }

        for (User recipient : keyRecipients) {
            assignKeyToUser(zebedee, recipient, collection.getDescription().getId(), key);
        }

        zebedee.getKeyringCache()
                .getSchedulerCache()
                .put(collection.getDescription().getId(), key);
    }

    private static <T> List<T> nullSafeList(List<T> list) {
        if (list == null) {
            return new ArrayList<T>();
        }
        return list;
    }

    /**
     * Creates a {@link List} of {@link Callable} tasks to distribute/remove the collection key. For each user creates
     * either a key assignment task if they should have access to the collection, a key removal task if not and a single
     * task to add the new key to {@link Zebedee#keyringCache}
     *
     * @param zebedee         the {@link Zebedee} instance to use.
     * @param collection      the {@link Collection} the key belongs too.
     * @param secretKey       the {@link SecretKey} object to read the collection.
     * @param isNewCollection true if the collection is new, false otherwise.
     * @return A list of {@link Callable}'s which will assign/remove/cache the key as necessary.
     * @throws IOException
     */
    public static List<Callable<Boolean>> getKeyAssignmentTasks(Zebedee zebedee, Collection collection, SecretKey secretKey, boolean isNewCollection) throws IOException {
        List<User> keyRecipients = zebedee.getPermissionsService().getCollectionAccessMapping(collection);
        List<Callable<Boolean>> collectionKeyTasks = new ArrayList<>();

        if (!isNewCollection) {
            // Filter out the users who are should not receive the key and take it from them [evil laugh].
            zebedee.getUsersService().list().stream().filter(user -> !keyRecipients.contains(user)).forEach(nonKeyRecipient -> {
                collectionKeyTasks.add(() -> {
                    removeKeyFromUser(zebedee, nonKeyRecipient, collection.getDescription().getId());
                    return true;
                });
            });
        }

        // Add the key to each recipient user.
        keyRecipients.stream().forEach(user -> {
            collectionKeyTasks.add(() -> {
                assignKeyToUser(zebedee, user, collection.getDescription().getId(), secretKey);
                return true;
            });
        });

        // Put the Key in the schedule cache.
        collectionKeyTasks.add(() -> {
            zebedee.getKeyringCache().getSchedulerCache().put(collection.getDescription().getId(), secretKey);
            return true;
        });
        return collectionKeyTasks;
    }

    public static void distributeApplicationKey(Zebedee zebedee, String application, SecretKey secretKey) throws IOException {
        for (User user : zebedee.getUsersService().list()) {
            distributeApplicationKeyToUser(zebedee, application, secretKey, user);
        }
    }

    private static void distributeApplicationKeyToUser(Zebedee zebedee, String application, SecretKey secretKey, User user) throws IOException {
        if (user == null || StringUtils.isEmpty(user.getEmail())) {
            return;
        }
        if (userShouldHaveApplicationKey(zebedee, user)) {
            // Add the key
            assignKeyToUser(zebedee, user, application, secretKey);
        } else {
            removeKeyFromUser(zebedee, user, application);
        }
    }


    private static boolean userShouldHaveApplicationKey(Zebedee zebedee, User user) throws IOException {
        return zebedee.getPermissionsService().isAdministrator(user.getEmail()) || zebedee.getPermissionsService().canEdit(user.getEmail());
    }

    /**
     * Determine if the user should have the key assigned or removed for the given collection.
     *
     * @param zebedee
     * @param collection
     * @param session
     * @param user
     * @throws IOException
     */
    public static void distributeKeyToUser(Zebedee zebedee, Collection collection, Session session, User user) throws IOException {
        SecretKey key = zebedee.getKeyringCache().get(session).get(collection.getDescription().getId());
        distributeKeyToUser(zebedee, collection, key, user);
    }

    private static void distributeKeyToUser(Zebedee zebedee, Collection collection, SecretKey key, User user) throws IOException {
        if (userShouldHaveKey(zebedee, user, collection)) {
            // Add the key
            assignKeyToUser(zebedee, user, collection.getDescription().getId(), key);
        } else {
            removeKeyFromUser(zebedee, user, collection.getDescription().getId());
        }
    }

    /**
     * @param zebedee
     * @param user
     * @param key
     * @throws IOException
     */
    public static void assignKeyToUser(Zebedee zebedee, User user, String keyIdentifier, SecretKey key) throws IOException {
        // Escape in case user keyring has not been generated
        if (user.keyring() == null) {
            logWarn("Skipping assigning collection key to user as their keyring has not been initialized.")
                    .user(user.getEmail())
                    .collectionId(keyIdentifier)
                    .log();
            return;
        }

        zebedee.getUsersService().addKeyToKeyring(user.getEmail(), keyIdentifier, key);

        // If the user is logged in assign the key to their cached keyring
        Session session = zebedee.getSessionsService().find(user.getEmail());
        if (session != null) {
            Keyring keyring = zebedee.getKeyringCache().get(session);
            try {
                if (keyring != null) {
                    keyring.put(keyIdentifier, key);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Remove the collection key for the given user.
     * This method is intentionally private as the distribute* methods should be used
     * to re-evaluate whether a key should be removed instead of just removing it.
     *
     * @param zebedee
     * @param user
     * @throws IOException
     */
    private static void removeKeyFromUser(Zebedee zebedee, User user, String keyIdentifier) throws IOException {
        // Escape in case user keyring has not been generated
        if (user.keyring() == null) {
            return;
        }
        zebedee.getUsersService().removeKeyFromKeyring(user.getEmail(), keyIdentifier);

        // If the user is logged in remove the key from their cached keyring
        Session session = zebedee.getSessionsService().find(user.getEmail());
        if (session != null) {
            Keyring keyring = zebedee.getKeyringCache().get(session);
            try {
                if (keyring != null) {
                    keyring.remove(keyIdentifier);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Transfer a set of secret keys from the source keyring to the target
     *
     * @param targetKeyring the keyring to be populated
     * @param sourceKeyring the keyring to take keys from
     * @param collectionIds the keys to transfer
     * @throws NotFoundException
     * @throws BadRequestException
     * @throws IOException
     */
    public static void transferKeyring(Keyring targetKeyring, Keyring sourceKeyring, Set<String> collectionIds)
            throws NotFoundException, BadRequestException, IOException {

        for (String collectionId : collectionIds) {
            SecretKey key = sourceKeyring.get(collectionId);
            if (key != null) {
                targetKeyring.put(collectionId, key);
            }
        }
    }

    /**
     * Transfer all secret keys from the source keyring to the target
     *
     * @param targetKeyring the keyring to be populated
     * @param sourceKeyring the keyring to take keys from
     * @throws NotFoundException
     * @throws BadRequestException
     * @throws IOException
     */
    public static void transferKeyring(Keyring targetKeyring, Keyring sourceKeyring)
            throws NotFoundException, BadRequestException, IOException {
        transferKeyring(targetKeyring, sourceKeyring, sourceKeyring.list());
    }

    private static boolean userShouldHaveKey(Zebedee z, User user, Collection collection) throws IOException {
        return z.getPermissionsService().isAdministrator(user.getEmail())
                || z.getPermissionsService().canView(user, collection.getDescription());
    }

    private static Collection getCollection(String id) {
        try {
            return collectionsServiceSupplier.getService().getCollection(id);
        } catch (IOException e) {
            logError(e, "failed to get collection").addParameter("collectionId", id).log();
            throw new RuntimeException("failed to get collection with collectionId " + id, e);
        }
    }

    public static void setZebedeeCmsService(ZebedeeCmsService zebedeeCmsService) {
        KeyManager.zebedeeCmsService = zebedeeCmsService;
    }
}
