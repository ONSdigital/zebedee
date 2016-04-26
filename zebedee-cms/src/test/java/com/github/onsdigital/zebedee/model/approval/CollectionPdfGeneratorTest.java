package com.github.onsdigital.zebedee.model.approval;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.DummyCollectionReader;
import com.github.onsdigital.zebedee.model.DummyCollectionWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
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
        CollectionWriter collectionWriter =  new DummyCollectionWriter(tempDirectory);

        generator.generatePdfsInCollection(collectionWriter, new ArrayList<>());
    }

    @Test
    public void shouldGeneratePdfForArticle() throws IOException, ZebedeeException {

        // given a faked collection with an article
        Path tempDirectory = Files.createTempDirectory(Random.id()); // create a temp directory to generate content into
        CollectionWriter collectionWriter =  new DummyCollectionWriter(tempDirectory);
        ArrayList<ContentDetail> collectionContent = new ArrayList<>();
        String uri = "/the/uri";
        collectionContent.add(new ContentDetail("Some article", uri, PageType.article.toString()));

        // when the generate PDF method is called.
        generator.generatePdfsInCollection(collectionWriter, collectionContent);

        // the expected file is generated in the reviewed section of the collection
        CollectionReader collectionReader = new DummyCollectionReader(tempDirectory);
        collectionReader.getResource(uri + "/page.pdf"); // would throw not found exception if not there.
    }

}
