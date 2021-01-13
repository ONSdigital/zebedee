package com.github.onsdigital.zebedee.keyring.util;

import org.apache.commons.io.FileUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

// Examples of AES encryption.
public class EncryptionExamples {

    static Path BASE_PATH = Paths.get("/Users/Dave/Desktop/crypto");
    static Path SECRET_KEY_FILE = BASE_PATH.resolve("secret-key");
    static Path IV_FILE = BASE_PATH.resolve("init-vector");
    static Path OUTPUT_FILE = BASE_PATH.resolve("encrypted-file.json");

    public static void main(String[] args) throws Exception {
        encrypt("You can't kill Joey Rammone");
        System.out.println("Decrypted file: " + decrypt());
    }

    static void encrypt(String message) throws Exception {
        SecretKey secretKey = loadSecretKey();
        IvParameterSpec iv = loadInitVector();

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

        byte[] encryptedBytes = cipher.doFinal(message.getBytes());
        FileUtils.writeByteArrayToFile(OUTPUT_FILE.toFile(), encryptedBytes);
    }

    static String decrypt() throws Exception {
        SecretKey secretKey = loadSecretKey();
        IvParameterSpec iv = loadInitVector();

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

        byte[] fileBytes = FileUtils.readFileToByteArray(OUTPUT_FILE.toFile());

        return new String(cipher.doFinal(fileBytes));
    }

    static SecretKey loadSecretKey() throws Exception {
        byte[] base64BYtes = FileUtils.readFileToByteArray(SECRET_KEY_FILE.toFile());
        byte[] secretkeyBytes = Base64.getDecoder().decode(base64BYtes);

        return new SecretKeySpec(secretkeyBytes, 0, secretkeyBytes.length, "AES");
    }

    static IvParameterSpec loadInitVector() throws Exception {
        byte[] base64BYtes = FileUtils.readFileToByteArray(IV_FILE.toFile());
        byte[] iv = Base64.getDecoder().decode(base64BYtes);

        return new IvParameterSpec(iv);
    }

    static void generateSecretKeyFile() throws Exception {
        SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();
        String secretkeyBase64 = Base64.getEncoder().encodeToString(secretKey.getEncoded());

        FileUtils.writeByteArrayToFile(SECRET_KEY_FILE.toFile(), secretkeyBase64.getBytes());
    }

    static void generateIV() throws Exception {
        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[16];
        secureRandom.nextBytes(iv);

        FileUtils.writeByteArrayToFile(IV_FILE.toFile(), Base64.getEncoder().encode(iv));
    }
}
