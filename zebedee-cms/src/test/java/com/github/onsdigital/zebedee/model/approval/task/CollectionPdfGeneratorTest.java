package com.github.onsdigital.zebedee.model.approval.task;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.approval.tasks.CollectionPdfGenerator;
import com.github.onsdigital.zebedee.service.PdfService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CollectionPdfGeneratorTest {

    @Mock
    private PdfService mockPDFService;

    @Mock
    private CollectionWriter mockCollectionWriter;

    @Mock
    private ContentWriter mockContentWriter;

    private CollectionPdfGenerator generator;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        generator = new CollectionPdfGenerator(mockPDFService);
    }


    @Test
    public void shouldGenerateNothingForAnEmptyCollection() throws IOException {
        generator.generatePdfsInCollection(mockCollectionWriter, new ArrayList<>());
        verifyZeroInteractions(mockPDFService);
    }

    @Test
    public void shouldGeneratePdfForArticle() throws IOException, ZebedeeException {
        when(mockCollectionWriter.getReviewed())
                .thenReturn(mockContentWriter);

        ArrayList<ContentDetail> collectionContent = new ArrayList<>();
        String uri = "/the/uri";
        collectionContent.add(new ContentDetail("Some article", uri, PageType.article.toString()));

        generator.generatePdfsInCollection(mockCollectionWriter, collectionContent);

        verify(mockCollectionWriter, times(1)).getReviewed();
    }
}
