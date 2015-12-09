package com.github.onsdigital.zebedee.util;

import org.apache.commons.io.IOUtils;

import javax.crypto.SecretKey;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utilities related to file compression.
 */
public class ZipUtils {

    /**
     * Unzip the given file into the given destination.
     *
     * @param zipFile
     * @param destination
     * @throws IOException
     */
    public static void unzip(final File zipFile, final String destination) throws IOException {
        unzip(new FileInputStream(zipFile), destination);
    }

    public static void unzip(final InputStream inputStream, final String destination) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null && !zipEntry.isDirectory()) {
                String name = zipEntry.getName();
                File file = new File(destination + File.separator + name);
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }

                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    IOUtils.copy(zipInputStream, fileOutputStream);
                }
            }
        }
    }

    /**
     * Zip the given folder into the given zip file.
     *
     * @param folder
     * @param zipFile
     * @throws IOException
     */
    public static void zipFolder(final File folder, final File zipFile) throws IOException {
        zipFolder(folder, new FileOutputStream(zipFile));
    }

    private static void zipFolder(final File folder, final OutputStream outputStream) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            zipFolder(folder, zipOutputStream, folder.getPath().length() + 1);
        }
    }

    private static void zipFolder(final File folder, final ZipOutputStream zipOutputStream, final int prefixLength)
            throws IOException {
        for (final File file : folder.listFiles()) {
            if (file.isFile()) {
                final ZipEntry zipEntry = new ZipEntry(file.getPath().substring(prefixLength));
                zipOutputStream.putNextEntry(zipEntry);
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    IOUtils.copy(inputStream, zipOutputStream);
                }
                zipOutputStream.closeEntry();
            } else if (file.isDirectory()) {
                zipFolder(file, zipOutputStream, prefixLength);
            }
        }
    }

    public static void zipFolderWithEncryption(final File folder, final File zipFile, SecretKey key) throws IOException {
        zipFolderWithEncryption(folder, EncryptionUtils.encryptionOutputStream(zipFile.toPath(), key), key);
    }
    private static void zipFolderWithEncryption(final File folder, final OutputStream outputStream, SecretKey key) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            zipFolderWithEncryption(folder, zipOutputStream, key, folder.getPath().length() + 1);
        }
    }

    private static void zipFolderWithEncryption(final File folder, final ZipOutputStream zipOutputStream, SecretKey key, final int prefixLength)
            throws IOException {
        for (final File file : folder.listFiles()) {
            if (file.isFile()) {
                final ZipEntry zipEntry = new ZipEntry(file.getPath().substring(prefixLength));
                zipOutputStream.putNextEntry(zipEntry);
                try (InputStream inputStream = EncryptionUtils.encryptionInputStream(file.toPath(), key)) {
                    IOUtils.copy(inputStream, zipOutputStream);
                }
                zipOutputStream.closeEntry();
            } else if (file.isDirectory()) {
                zipFolderWithEncryption(file, zipOutputStream, key, prefixLength);
            }
        }
    }
}
