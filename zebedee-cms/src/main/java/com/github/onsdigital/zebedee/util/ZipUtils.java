package com.github.onsdigital.zebedee.util;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utilities related to file compression.
 */
public class ZipUtils {

    public static void unzip(final File zipFile, final String destination) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null && !zipEntry.isDirectory()) {
                String name = zipEntry.getName();
                long size = zipEntry.getSize();
                long compressedSize = zipEntry.getCompressedSize();
                Log.print("name: %-20s | size: %6d | compressed size: %6d\n",
                        name, size, compressedSize);

                File file = new File(destination + File.separator + name);
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }

                FileOutputStream fileOutputStream = new FileOutputStream(file);
                IOUtils.copy(zipInputStream, fileOutputStream);
            }
        }
    }

    public static void zipFolder(final File folder, final File zipFile) throws IOException {
        zipFolder(folder, new FileOutputStream(zipFile));
    }

    public static void zipFolder(final File folder, final OutputStream outputStream) throws IOException {
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
}
