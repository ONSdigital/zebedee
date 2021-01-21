package com.github.onsdigital.zebedee.keyring.store;

import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.apache.commons.io.IOUtils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * {@link java.io.File} based implementation of {@link CollectionKeyStore}. Reads and writes
 * {@link SecretKey} objects to/from encrypted files on disk. Methods read, write & delete all acquire an exclusive
 * {@link java.nio.channels.FileLock} when accessing the key files. <p><b>We strongly advised against using/extending
 * this code for anything other than maintaining legacy functionality in Zebedee CMS.</b></p>For all other purposes
 * it should be considered deprecated.
 */
public class CollectionKeyStoreImpl implements CollectionKeyStore {

    static final String INVALID_COLLECTION_ID_ERR = "collectionID required but was null or empty";
    static final String COLLECTION_KEY_NULL_ERR = "collectionKey required but was null";
    static final String COLLECTION_KEY_NOT_FOUND_ERR = "collectionKey not found";
    static final String KEY_DECRYPTION_ERR = "error while decrypting collectionKey file";
    static final String WRITE_KEY_ERR = "error writing collection key to store";
    static final String COLLECTION_KEY_ALREADY_EXISTS_ERR = "collectionKey for this collection ID already exists";
    static final String ENCRYPTION_ALGORITHM = "AES";
    static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    static final String FILE_ACCESS_MODE = "rw";

    private Path keyringDir;
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
    public SecretKey read(final String collectionID) throws KeyringException {
        validateRead(collectionID);

        byte[] keyBytes = null;
        try (
                RandomAccessFile raf = new RandomAccessFile(getKeyPath(collectionID), FILE_ACCESS_MODE);
                FileChannel channel = raf.getChannel();
                InputStream in = Channels.newInputStream(channel);
                CipherInputStream cin = new CipherInputStream(in, getDecryptCipher());
        ) {
            // FileLock documentation states the lock will released when the FileChannel used to acquire it is closed.
            // FileChannel implements autoclosable and in this case is defined inside a try-with-resources which
            // guarantees auto closeable resourses are closed.
            channel.lock();
            keyBytes = IOUtils.toByteArray(cin);
            return new SecretKeySpec(keyBytes, 0, keyBytes.length, ENCRYPTION_ALGORITHM);
        } catch (Exception ex) {
            throw new KeyringException(KEY_DECRYPTION_ERR, collectionID, ex);
        } finally {
            if (keyBytes != null) {
                destoryKeyBytes(keyBytes);
            }
        }
    }

    @Override
    public void write(final String collectionID, final SecretKey collectionKey) throws KeyringException {
        validateWrite(collectionID, collectionKey);

        try (
                RandomAccessFile randomAccessFile = new RandomAccessFile(getKeyPath(collectionID), FILE_ACCESS_MODE);
                FileChannel channel = randomAccessFile.getChannel();
                OutputStream out = Channels.newOutputStream(channel);
                CipherOutputStream cos = new CipherOutputStream(out, getEncryptCipher())
        ) {
            // FileLock documentation states the lock will released when the FileChannel used to acquire it is closed.
            // FileChannel implements autoclosable and in this case is defined inside a try-with-resources which
            // guarantees auto closeable resourses are closed.
            channel.lock();
            cos.write(collectionKey.getEncoded());
            cos.flush();
        } catch (Exception ex) {
            throw new KeyringException(WRITE_KEY_ERR, collectionID, ex);
        }
    }

    @Override
    public void delete(String collectionID) throws KeyringException {
        // TODO implementation coming soon.
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

        Path keyPath = Paths.get(getKeyPath(collectionID));

        if (Files.notExists(keyPath)) {
            throw new KeyringException(COLLECTION_KEY_NOT_FOUND_ERR, collectionID);
        }
    }

    private void validateWrite(String collectionID, SecretKey collectionKey) throws KeyringException {
        if (isEmpty(collectionID)) {
            throw new KeyringException(INVALID_COLLECTION_ID_ERR);
        }

        Path keyPath = Paths.get(getKeyPath(collectionID));
        if (Files.exists(keyPath)) {
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
     * object remains in memory it is technically possible for someone retieve it using Java memory monitoring tools.
     * This is very very unlikely but as an additional safeguard to minimise the window of opportunity we "zero" the
     * SecretKey data as soon as we have finished using it ensuring its only available for the time its required.
     *
     * @param keyBytes the array of bytes "destroy".
     */
    private void destoryKeyBytes(byte[] keyBytes) {
        Arrays.fill(keyBytes, (byte) 0);
    }

}
