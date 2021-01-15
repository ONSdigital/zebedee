package com.github.onsdigital.zebedee.keyring.store;

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

public class CollectionKeyStoreImpl implements CollectionKeyStore {

    private Path keyringDir;
    private SecretKey masterKey;
    private IvParameterSpec masterIv;
    private Gson gson;

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

    @Override
    public void write(CollectionKey key) throws KeyringException {
        // TODO validate key
        try (
                FileOutputStream fos = new FileOutputStream(getKeyPath(key.getCollectionID()));
                CipherOutputStream cos = new CipherOutputStream(fos, getEncryptCipher())
        ) {
            cos.write(gson.toJson(key).getBytes());
            cos.flush();
        } catch (Exception ex) {
            throw new KeyringException(ex);
        }
    }

    private String getKeyPath(String collectionID) {
        return keyringDir.resolve(collectionID + ".json").toString();
    }

    private Cipher getEncryptCipher() throws KeyringException {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, masterIv);
            return cipher;
        } catch (Exception ex) {
            throw new KeyringException(ex);
        }
    }

    private Cipher getDecryptCipher() throws KeyringException {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, masterKey, masterIv);
            return cipher;
        } catch (Exception ex) {
            throw new KeyringException(ex);
        }
    }
}
