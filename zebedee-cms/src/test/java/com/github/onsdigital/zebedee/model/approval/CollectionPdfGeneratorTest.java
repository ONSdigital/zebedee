package com.github.onsdigital.zebedee.model.approval;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.FakeCollectionWriter;
import com.github.onsdigital.zebedee.service.DummyPdfService;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class CollectionPdfGeneratorTest {

    CollectionPdfGenerator generator = new CollectionPdfGenerator(new DummyPdfService());

    @Test
    public void shouldGenerateNothingForAnEmptyCollection() throws IOException, NotFoundException, BadRequestException, UnauthorizedException {

        String id = Random.id();
        Path tempDirectory = Files.createTempDirectory(id);
        Path collectionDirectory = tempDirectory.resolve("collectionId");
        Files.createDirectory(collectionDirectory);
        Files.createFile(tempDirectory.resolve("collectionId.json"));
        CollectionWriter collectionWriter =  new FakeCollectionWriter(tempDirectory.toString(), id);

        generator.generatePdfsInCollection(collectionWriter, new ArrayList<>());
    }

    public void shouldGeneratePdfForArticle() throws IOException, NotFoundException, BadRequestException, UnauthorizedException {

        String id = Random.id();
        Path tempDirectory = Files.createTempDirectory(id);
        CollectionWriter collectionWriter =  new FakeCollectionWriter(tempDirectory.toString(), id);

        generator.generatePdfsInCollection(collectionWriter, new ArrayList<>());
    }

}
