package com.github.onsdigital.zebedee.model.approval.task;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.approval.tasks.CollectionPdfGenerator;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;
import com.github.onsdigital.zebedee.service.PdfService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class CollectionPdfGeneratorTest {

    @Mock
    private PdfService pdfService;

    @Mock
    private ContentReader contentReader;

    @Mock
    private ContentWriter contentWriter;

    @Mock
    private Collection collection;

    private CollectionPdfGenerator generator;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        generator = new CollectionPdfGenerator(pdfService);
    }

    @Test
    public void shouldGenerateNothingForAnEmptyCollection() throws Exception {
        generator.generatePDFsForCollection(collection, contentReader, contentWriter, new ArrayList<>());
        verifyNoInteractions(pdfService);
    }

    @Test
    public void shouldGeneratePdfForContentWithoutExplicitLanguage() throws IOException, ZebedeeException {
        List<ContentDetail> collectionContent = new ArrayList<>();
        String uri = "/the/uri";
        
        ContentDetail content = new ContentDetail("Some article", uri, PageType.ARTICLE);
        content.description.language = null;
        collectionContent.add(content);

        // Mock there is not a Welsh resource for this content
        when(contentReader.getResource(uri+"/data_cy.json")).thenThrow(new NotFoundException("Welsh data file not found"));

        generator.generatePDFsForCollection(collection, contentReader, contentWriter, collectionContent);

        // Verify it generates the English version only
        verify(pdfService, times(1)).generatePdf(contentWriter, uri, ContentLanguage.en);
        verify(pdfService, never()).generatePdf(contentWriter, uri, ContentLanguage.cy);
    }

    @Test
    public void shouldGeneratePdfForEnglishOnlyContent() throws IOException, ZebedeeException {
        List<ContentDetail> collectionContent = new ArrayList<>();
        String uri = "/the/uri";
        
        ContentDetail content = new ContentDetail("Some article", uri, PageType.ARTICLE);
        content.description.language = ContentLanguage.en.getId();
        collectionContent.add(content);

        // Mock there is not a Welsh resource for this content
        when(contentReader.getResource(uri+"/data_cy.json")).thenThrow(new NotFoundException("Welsh data file not found"));

        generator.generatePDFsForCollection(collection, contentReader, contentWriter, collectionContent);

        // Verify it generates the English version only
        verify(pdfService, times(1)).generatePdf(contentWriter, uri, ContentLanguage.en);
        verify(pdfService, never()).generatePdf(contentWriter, uri, ContentLanguage.cy);
    }

    @Test
    public void shouldGeneratePdfForWelshOnlyContent() throws IOException, ZebedeeException {
        List<ContentDetail> collectionContent = new ArrayList<>();
        String uri = "/the/uri";
        
        ContentDetail content = new ContentDetail("Some article", uri, PageType.ARTICLE);
        content.description.language = ContentLanguage.cy.getId();
        collectionContent.add(content);

        generator.generatePDFsForCollection(collection, contentReader, contentWriter, collectionContent);

        // If a Welsh version is requested, it means there is no English version (as it would override it)
        verify(contentReader, never()).getResource(uri+"/data_cy.json");
        // Verify it generates the Welsh version only
        verify(pdfService, times(1)).generatePdf(contentWriter, uri, ContentLanguage.cy);
        verify(pdfService, never()).generatePdf(contentWriter, uri, ContentLanguage.en);
    }

    @Test
    public void shouldGeneratePdfForEnglishAndWelshContent() throws IOException, ZebedeeException {
        List<ContentDetail> collectionContent = new ArrayList<>();
        String uri = "/the/uri";
        
        ContentDetail content = new ContentDetail("Some article", uri, PageType.ARTICLE);
        content.description.language = ContentLanguage.en.getId();
        collectionContent.add(content);

        // Mock there is a Welsh resource for this content
        when(contentReader.getResource(uri+"/data_cy.json")).thenReturn(new Resource());

        generator.generatePDFsForCollection(collection, contentReader, contentWriter, collectionContent);

        // Verify it generates the English and Welsh version
        verify(pdfService, times(1)).generatePdf(contentWriter, uri, ContentLanguage.cy);
        verify(pdfService, times(1)).generatePdf(contentWriter, uri, ContentLanguage.en);
    }
}
