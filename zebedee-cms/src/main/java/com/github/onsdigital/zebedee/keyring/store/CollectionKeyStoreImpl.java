package com.github.onsdigital.zebedee.keyring.store;

import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.units.qual.K;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * {@link java.io.File} based implementation of {@link CollectionKeyStore}. Reads and writes
 * {@link SecretKey} objects to/from encrypted files on disk. Each method employs a synchronized block to prevent
 * race conditions whilst accessing the key files. This negative performance impact is a necessary and unavoidable
 * drawback to using files on disk instead of database.
 *
 * <p><b>We strongly advised against using/extending this code for anything other than maintaining legacy
 * functionality in Zebedee CMS.</b></p>For all other purposes it should be considered deprecated. Use at your own risk.
 */
public class CollectionKeyStoreImpl implements CollectionKeyStore {

    static final String INVALID_COLLECTION_ID_ERR = "collectionID required but was null or empty";
    static final String COLLECTION_KEY_NULL_ERR = "collectionKey required but was null";
    static final String COLLECTION_KEY_NOT_FOUND_ERR = "collectionKey not found";
    static final String KEY_DECRYPTION_ERR = "error while decrypting collectionKey file";
    static final String WRITE_KEY_ERR = "error writing collection key to store";
    static final String COLLECTION_KEY_ALREADY_EXISTS_ERR = "collectionKey for this collection ID already exists";
    static final String FAILED_TO_DELETE_COLLECTION_KEY_ERR = "failed to delete collection key";
    static final String ENCRYPTION_ALGORITHM = "AES";
    static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";


    private Path keyringDir;
    private SecretKey masterKey;
    private IvParameterSpec masterIv;

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
     * @param keyringDir the {@link Path} to read the collection encryption keys files from.
     * @param masterKey  the {@link SecretKey} to use when decrypting the collection key files.
     * @param masterIv   the {@link IvParameterSpec} to use when initializing the encryption {@link Cipher}.
     */
    public CollectionKeyStoreImpl(final Path keyringDir, final SecretKey masterKey, final IvParameterSpec masterIv) {
        this.keyringDir = keyringDir;
        this.masterKey = masterKey;
        this.masterIv = masterIv;
    }

    @Override
    public synchronized boolean exists(String collectionID) throws KeyringException {
        if (isEmpty(collectionID)) {
            throw new KeyringException(INVALID_COLLECTION_ID_ERR);
        }

        return Files.exists(Paths.get(getKeyPath(collectionID)));
    }

    @Override
    public synchronized SecretKey read(final String collectionID) throws KeyringException {
        validateRead(collectionID);
        return readKeyFromFile(collectionID);
    }

    private SecretKey readKeyFromFile(final String collectionID) throws KeyringException {
        byte[] keyBytes = null;
        try (
                FileInputStream fin = new FileInputStream(getKeyPath(collectionID));
                CipherInputStream cin = new CipherInputStream(fin, getDecryptCipher());
        ) {
            keyBytes = IOUtils.toByteArray(cin);
            return new SecretKeySpec(keyBytes, 0, keyBytes.length, ENCRYPTION_ALGORITHM);
        } catch (Exception ex) {
            throw new KeyringException(KEY_DECRYPTION_ERR, collectionID, ex);
        } finally {
            if (keyBytes != null) {
                destroyKeyBytes(keyBytes);
            }
        }
    }

    @Override
    public synchronized void write(final String collectionID, final SecretKey collectionKey) throws KeyringException {
        validateWrite(collectionID, collectionKey);
        writeKeyToFile(collectionID, collectionKey);
    }

    private void writeKeyToFile(final String collectionID, final SecretKey collectionKey) throws KeyringException {
        try (
                FileOutputStream fos = new FileOutputStream(getKeyPath(collectionID));
                CipherOutputStream cos = new CipherOutputStream(fos, getEncryptCipher())
        ) {
            cos.write(collectionKey.getEncoded());
            cos.flush();
        } catch (Exception ex) {
            throw new KeyringException(WRITE_KEY_ERR, collectionID, ex);
        }
    }

    @Override
    public synchronized void delete(String collectionID) throws KeyringException {
        if (isEmpty(collectionID)) {
            throw new KeyringException(INVALID_COLLECTION_ID_ERR);
        }

        if (!exists(collectionID)) {
            throw new KeyringException(COLLECTION_KEY_NOT_FOUND_ERR, collectionID);
        }

        Path keyPath = Paths.get(getKeyPath(collectionID));
        try {
            if (!Files.deleteIfExists(keyPath)) {
                throw new KeyringException(FAILED_TO_DELETE_COLLECTION_KEY_ERR, collectionID);
            }
        } catch (IOException ex) {
            throw new KeyringException(FAILED_TO_DELETE_COLLECTION_KEY_ERR, collectionID, ex);
        }
    }

    private String getKeyPath(String collectionID) {
        return keyringDir.resolve(collectionID + ".key").toString();
    }

    private Cipher getDecryptCipher() throws KeyringException {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, masterIv);
            return cipher;
        } catch (Exception ex) {
            throw new KeyringException(ex);
        }
    }

    private void validateRead(String collectionID) throws KeyringException {
        if (isEmpty(collectionID)) {
            throw new KeyringException(INVALID_COLLECTION_ID_ERR);
        }

        if (!exists(collectionID)) {
            throw new KeyringException(COLLECTION_KEY_NOT_FOUND_ERR, collectionID);
        }
    }

    private void validateWrite(String collectionID, SecretKey collectionKey) throws KeyringException {
        if (isEmpty(collectionID)) {
            throw new KeyringException(INVALID_COLLECTION_ID_ERR);
        }

        Path keyPath = Paths.get(getKeyPath(collectionID));
        if (exists(collectionID)) {
            throw new KeyringException(COLLECTION_KEY_ALREADY_EXISTS_ERR, collectionID);
        }

        if (collectionKey == null) {
            throw new KeyringException(COLLECTION_KEY_NULL_ERR, collectionID);
        }
    }

    private Cipher getEncryptCipher() throws KeyringException {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, masterIv);
            return cipher;
        } catch (Exception ex) {
            throw new KeyringException(ex);
        }
    }

    /**
     * Even after an object has gone out of scope it will remain on the heap until GC runs to release it. While the
     * object remains in memory it is technically possible for someone retrieve it using Java memory monitoring tools.
     * This is very very unlikely but as an additional safeguard to minimise the window of opportunity we "zero" the
     * SecretKey data as soon as we have finished using it ensuring its only available for the time its required.
     *
     * @param keyBytes the array of bytes "destroy".
     */
    private void destroyKeyBytes(byte[] keyBytes) {
        Arrays.fill(keyBytes, (byte) 0);
    }

}
