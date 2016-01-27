package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.ResourceUtils;
import com.github.davidcarboni.cryptolite.Random;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ZipUtilsTest {

    private Path rootPath;


    @Before
    public void setUp() throws Exception {
        rootPath = Files.createTempDirectory("ZipUtilsTest");
        System.out.println("setup: " + rootPath);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(rootPath.toFile());
    }

    @Test
    public void zipFolderShouldCreateZipFileOfFolder() throws IOException {

        File folderToZip = Files.createDirectory(rootPath.resolve("folderToZip")).toFile();
        Files.copy(ResourceUtils.getFile("/xls/example-table.xls").toPath(), folderToZip.toPath().resolve("example-table.xls"));
        File zipFile = rootPath.resolve(Random.id() + ".zip").toFile();

        Assert.assertFalse(zipFile.exists());

        ZipUtils.zipFolder(folderToZip, zipFile);

        Assert.assertTrue(zipFile.exists());
    }

    @Test
    public void zipFolderWithFilterShouldCreateZipFileOfFolder() throws IOException {

        File folderToZip = Files.createDirectory(rootPath.resolve("folderToZip")).toFile();
        Files.copy(ResourceUtils.getFile("/xls/example-table.xls").toPath(), folderToZip.toPath().resolve("example-table.xls"));
        Files.copy(ResourceUtils.getFile("/xls/example-table.xls").toPath(), folderToZip.toPath().resolve("FILTER-ME.xls"));
        File zipFile = rootPath.resolve(Random.id() + ".zip").toFile();

        Assert.assertFalse(zipFile.exists());

        ZipUtils.zipFolder(folderToZip, zipFile, uri -> uri.contains("FILTER-ME")); // apply a filter that always returns false, preventing any files being included.

        Assert.assertTrue(zipFile.exists());

        Path unzippedFolder = rootPath.resolve("unzipped");
        ZipUtils.unzip(zipFile, unzippedFolder.toString());

        Assert.assertTrue(unzippedFolder.toFile().exists());
        Assert.assertTrue(unzippedFolder.resolve("example-table.xls").toFile().exists());
        Assert.assertFalse(unzippedFolder.resolve("FILTER-ME.xls").toFile().exists());
    }

    @Test
    public void unzipShouldUnzipAsExpected() throws IOException {

        File folderToZip = Files.createDirectory(rootPath.resolve("folderToZip")).toFile();
        Files.copy(ResourceUtils.getFile("/xls/example-table.xls").toPath(), folderToZip.toPath().resolve("example-table.xls"));
        File zipFile = rootPath.resolve(Random.id() + ".zip").toFile();
        ZipUtils.zipFolder(folderToZip, zipFile);

        Path unzippedFolder = rootPath.resolve("unzipped");
        ZipUtils.unzip(zipFile, unzippedFolder.toString());

        Assert.assertTrue(unzippedFolder.toFile().exists());
        Assert.assertTrue(unzippedFolder.resolve("example-table.xls").toFile().exists());
    }
}
