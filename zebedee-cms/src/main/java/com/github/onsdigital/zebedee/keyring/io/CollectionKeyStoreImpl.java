package com.github.onsdigital.zebedee.keyring.io;

import com.github.onsdigital.zebedee.keyring.CollectionKey;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * {@link java.io.File} based implementation of {@link CollectionKeyStore}. Reads and writes
 * {@link CollectionKey} objects to/from encrypted files on disk. Methods read, write & delete all aquire and exclusive
 * {@link java.nio.channels.FileLock} when accessing the collection key files.
 * <p><b>We strongly advised against using/extending this
 * code for anything other than maintaining legacy functionality in Zebedee CMS.</b></p>For all other purposes it
 * should be considered deprecated.
 */
public class CollectionKeyStoreImpl implements CollectionKeyStore {

    static final String INVALID_COLLECTION_ID_ERR = "collectionID required but was null or empty";
    static final String COLLECTION_KEY_NULL_ERR = "collectionKey required but was null";
    static final String COLLECTION_KEY_NOT_FOUND_ERR = "collectionKey not found";
    static final String KEY_DECRYPTION_ERR = "error while decrypting collectionKey file";
    static final String WRITE_KEY_ERR = "error writing collection key to store";
    static final String COLLECTION_KEY_ID_NULL_ERR = "collectionKey.ID required but was null or empty";
    static final String COLLECTION_KEY_KEY_NULL_ERR = "collectionKey.secretKey required but was null";
    static final String COLLECTION_KEY_ALREADY_EXISTS_ERR = "collectionKey for this collection ID already exists";
    static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private Path keyringDir;
    private Gson gson;
    private transient SecretKey masterKey;
    private transient IvParameterSpec masterIv;

    /**
     * <p>
     * Create a new CollectionKeyStore instance.
     * </p>
     *
     * <p>
     * <b>Important:</b> It is strongly recommended that the {@link SecretKey} & {@link IvParameterSpec}
     * are not modified once they have been used to store collection keys. Changing these inputs will result in this
     * object no longer being able to decrypt collection key files written with the previous encryption key.
     * <p>
     * Reverting the key/init vector values will resolve this issue for keys written with the original encryption
     * parameters however, the same issue will now be the case for any key files written using the modified encryption
     * params.
     * </p>
     *
     * <p>
     * <b>
     * If a encryption parameters change is necessary it is strongly recommended you migrate all existing keys to
     * use the updated encryption values.</b>
     * </p>
     *
     * @param keyringDir the {@link Path} the read the {@link CollectionKey} files from.
     * @param masterKey  the {@link SecretKey} to use when decrypting the {@link CollectionKey} files.
     * @param masterIv   the {@link IvParameterSpec} to use when initializing the encryption {@link Cipher}.
     */
    public CollectionKeyStoreImpl(final Path keyringDir, final SecretKey masterKey, final IvParameterSpec masterIv) {
        this.keyringDir = keyringDir;
        this.masterKey = masterKey;
        this.masterIv = masterIv;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(CollectionKey.class, new CollectionKeySerializer())
                .create();
    }

    @Override
    public CollectionKey read(String collectionID) throws KeyringException {
        if (isEmpty(collectionID)) {
            throw new KeyringException(INVALID_COLLECTION_ID_ERR);
        }

        Path keyPath = Paths.get(getKeyPath(collectionID));

        if (Files.notExists(keyPath)) {
            throw new KeyringException(COLLECTION_KEY_NOT_FOUND_ERR, collectionID);
        }

        try (
                RandomAccessFile raf = new RandomAccessFile(getKeyPath(collectionID), "rwd");
                FileChannel channel = raf.getChannel();
                InputStream in = Channels.newInputStream(channel);
                CipherInputStream cin = new CipherInputStream(in, getDecryptCipher());
                InputStreamReader reader = new InputStreamReader(cin);
        ) {
            // FileLock documentation states the lock will released when the FileChannel used to aquire it is closed.
            // FileChannel implements autoclosable and in this case is defined inside a try-with-resources which
            // guarantees auto closeable resourses are closed.
            channel.lock();
            return gson.fromJson(reader, CollectionKey.class);
        } catch (Exception ex) {
            throw new KeyringException(KEY_DECRYPTION_ERR, collectionID, ex);
        }
    }

    private String getKeyPath(String collectionID) {
        return keyringDir.resolve(collectionID + ".json").toString();
    }

    private Cipher getDecryptCipher() throws KeyringException {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, masterIv);
            return cipher;
        } catch (Exception ex) {
            throw new KeyringException(ex);
        }
    }

    @Override
    public void write(final CollectionKey key) throws KeyringException {
        validateCollectionKey(key);

        try (
                RandomAccessFile randomAccessFile = new RandomAccessFile(getKeyPath(key.getCollectionID()), "rwd");
                FileChannel channel = randomAccessFile.getChannel();
                OutputStream out = Channels.newOutputStream(channel);
                CipherOutputStream cos = new CipherOutputStream(out, getEncryptCipher())
        ) {
            // FileLock documentation states the lock will released when the FileChannel used to aquire it is closed.
            // FileChannel implements autoclosable and in this case is defined inside a try-with-resources which
            // guarantees auto closeable resourses are closed.
            channel.lock();
            cos.write(gson.toJson(key).getBytes());
            cos.flush();
        } catch (Exception ex) {
            throw new KeyringException(WRITE_KEY_ERR, key.getCollectionID(), ex);
        }
    }

    @Override
    public void delete(String collectionID) throws KeyringException {
        if (isEmpty(collectionID)) {
            throw new KeyringException(INVALID_COLLECTION_ID_ERR);
        }
    }

    private void validateCollectionKey(CollectionKey key) throws KeyringException {
        if (key == null) {
            throw new KeyringException(COLLECTION_KEY_NULL_ERR);
        }

        if (isEmpty(key.getCollectionID())) {
            throw new KeyringException(COLLECTION_KEY_ID_NULL_ERR);
        }

        Path keyPath = Paths.get(getKeyPath(key.getCollectionID()));
        if (Files.exists(keyPath)) {
            throw new KeyringException(COLLECTION_KEY_ALREADY_EXISTS_ERR, key.getCollectionID());
        }

        if (key.getSecretKey() == null) {
            throw new KeyringException(COLLECTION_KEY_KEY_NULL_ERR, key.getCollectionID());
        }
    }

    private Cipher getEncryptCipher() throws KeyringException {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, masterIv);
            return cipher;
        } catch (Exception ex) {
            throw new KeyringException(ex);
        }
    }

}
