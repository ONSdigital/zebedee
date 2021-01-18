package com.github.onsdigital.zebedee.keyring.io;

import com.github.onsdigital.zebedee.keyring.CollectionKey;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@link java.io.File} based implementation of {@link CollectionKeyReadWriter}. Reads and writes
 * {@link CollectionKey} objects to/from encrypted files on disk.
 * <p><b>We strongly advised against using/extending this
 * code for anything other than maintaining legacy functionality in Zebedee CMS.</b></p>For all other purposes it
 * should be considered deprecated.
 */
public class CollectionKeyReadWriterImpl implements CollectionKeyReadWriter {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private Path keyringDir;
    private SecretKey masterKey;
    private IvParameterSpec masterIv;
    private Gson gson;

    /**
     * <p>
     * Create a new readWriter instance.
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
    public CollectionKeyReadWriterImpl(final Path keyringDir, final SecretKey masterKey, final IvParameterSpec masterIv) {
        this.keyringDir = keyringDir;
        this.masterKey = masterKey;
        this.masterIv = masterIv;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(CollectionKey.class, new CollectionKeySerializer())
                .create();
    }

    @Override
    public CollectionKey read(String collectionID) throws KeyringException {
        if (StringUtils.isEmpty(collectionID)) {
            throw new KeyringException("collectionID required but was null or empty");
        }

        Path keyPath = Paths.get(getKeyPath(collectionID));

        if (Files.notExists(keyPath)) {
            throw new KeyringException("collectionKey not found", collectionID);
        }

        try (
                FileInputStream fin = new FileInputStream(getKeyPath(collectionID));
                CipherInputStream cin = new CipherInputStream(fin, getDecryptCipher());
                InputStreamReader reader = new InputStreamReader(cin);
        ) {
            return gson.fromJson(reader, CollectionKey.class);
        } catch (Exception ex) {
            throw new KeyringException("error while decrypting collectionKey file", collectionID, ex);
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
                FileOutputStream fos = new FileOutputStream(getKeyPath(key.getCollectionID()));
                CipherOutputStream cos = new CipherOutputStream(fos, getEncryptCipher())
        ) {
            cos.write(gson.toJson(key).getBytes());
            cos.flush();
        } catch (Exception ex) {
            throw new KeyringException("error writing collection key to store", key.getCollectionID(), ex);
        }
    }

    private void validateCollectionKey(CollectionKey key) throws KeyringException {
        if (key == null) {
            throw new KeyringException("collectionKey required but was null");
        }

        if (StringUtils.isEmpty(key.getCollectionID())) {
            throw new KeyringException("collectionKey.ID required but was null or empty");
        }

        if (key.getSecretKey() == null) {
            throw new KeyringException("collectionKey.secretKey required but was null", key.getCollectionID());
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
