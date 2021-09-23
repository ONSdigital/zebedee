package com.github.onsdigital.zebedee.keyring.util;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

import static java.text.MessageFormat.format;

/**
 * Util for generating a new {@link SecretKey} and {@link IvParameterSpec} required by the new central collection
 * keyring implementation.
 */
public class KeyringSecretsGenerator {

    /**
     * Generate new app secret config for the Central collection keyring.
     * <p>
     * Generates new SecretKey/IvParameterSpec values output as Base64 encode strings which can then be added to
     * application secrets JSON or local dev run.sh
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Generating new central keyring secrets config.\n");

        String key = generateNewSecretKey();
        String iv = generateNewIV();

        System.out.println("Add the following when updating the app secrets json:");
        System.out.println(format("\t\"KEYRING_SECRET_KEY\"=\"{0}\"", key));
        System.out.println(format("\t\"KEYRING_INIT_VECTOR\"=\"{0}\"", iv));

        System.out.println("\nOR\n");

        System.out.println("Add the follow to zebedee/run.sh for local dev set up:");
        System.out.println(format("\texport KEYRING_SECRET_KEY=\"{0}\"", key));
        System.out.println(format("\texport KEYRING_INIT_VECTOR=\"{0}\"", iv));
    }

    static String generateNewSecretKey() throws Exception {
        SecretKey key = KeyGenerator.getInstance("AES").generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    static String generateNewIV() {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        return Base64.getEncoder().encodeToString(new IvParameterSpec(iv).getIV());
    }

}
