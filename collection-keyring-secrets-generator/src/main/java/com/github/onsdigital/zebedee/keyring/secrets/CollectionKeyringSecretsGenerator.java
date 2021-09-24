package com.github.onsdigital.zebedee.keyring.secrets;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static java.text.MessageFormat.format;

/**
 * Util for generating a new {@link SecretKey} and {@link IvParameterSpec} required by the new central collection
 * keyring implementation.
 */
public class CollectionKeyringSecretsGenerator {

    static final String LOCAL = "local";
    static final String ENV = "env";

    /**
     * Generate new app secret config for the Central collection keyring.
     * <p>
     * Generates new SecretKey/IvParameterSpec values output as Base64 encode strings which can then be added to
     * application secrets JSON or local dev run.sh
     */
    public static void main(String[] args) throws Exception {
        String env = getEnv(args);
        if (env == null) {

            return;
        }

        String key = generateNewSecretKey();
        String iv = generateNewIV();

        switch (env) {
            case LOCAL:
                System.out.println();
                System.out.println("Add the follow to zebedee/run.sh for local dev set up:");
                System.out.println();
                System.out.println(format("  export KEYRING_SECRET_KEY=\"{0}\"", key));
                System.out.println(format("  export KEYRING_INIT_VECTOR=\"{0}\"", iv));
                System.out.println();
                break;
            case ENV:
                System.out.println();
                System.out.println("Add the following to app secrets json:");
                System.out.println();
                System.out.println(format("  \"KEYRING_SECRET_KEY\"=\"{0}\"", key));
                System.out.println(format("  \"KEYRING_INIT_VECTOR\"=\"{0}\"", iv));
                System.out.println();
                break;
            default:
                System.out.println();
                System.out.println("Please specify an environment arg:");
                System.out.println(" - \"env\" to generate secrets config for develop/production.");
                System.out.println(" - \"local\" to generate secrets config for local develop set up.");
                System.out.println();
        }
    }

    static String getEnv(String[] args) {
        if (args == null || args.length == 0) {
            return "";
        }

        return args[0];
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
