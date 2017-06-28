package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.document.article.Article;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.content.partial.markdown.MarkdownSection;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.session.model.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;


/**
 * Created by thomasridd on 16/11/15.
 */
public class CollectionMoveTest extends ZebedeeTestBaseFixture {

    Collection collection;
    Article martin;
    Article bedford;
    Article bedfordshire;
    Session session;

    public void setUp() throws Exception {
        session = zebedee.openSession(builder.publisher1Credentials);

        collection = new Collection(builder.collections.get(1), zebedee);
        martin = createArticle("/people/martin", "Martin");
        bedford = createArticle("/places/bedford", "Bedford");
        bedfordshire = createArticle("/places/bedfordshire", "Bedfordshire");

        savePages();
    }

    @Test
    public void shouldChangeReferencesInFileOnMoveContent() throws URISyntaxException, IOException, ZebedeeException {
        // Given
        // an item of content that references something
        martin.getRelatedArticles().add(new Link(bedford.getUri()));
        savePages();

        // When
        // we run the move
        collection.moveContent(session, "/places/bedford", "/places/london");

        // Then
        // the link should be updated;
        martin = (Article) readPageFromCollection(collection, martin.getUri().toString());
        assertEquals(1, martin.getRelatedArticles().size());
        assertEquals("/places/london", martin.getRelatedArticles().get(0).getUri().toString());
    }

    @Test
    public void shouldNotChangeExtendedReferencesInFileOnMoveContent() throws URISyntaxException, IOException, ZebedeeException {
        // Given
        // an item of content that references something
        martin.getRelatedArticles().add(new Link(new URI("/places/bedfordshire")));
        savePages();

        // When
        // we run the move
        collection.moveContent(session, "/places/bedford", "/places/london");

        // Then
        // the link should not be updated;
        martin = (Article) readPageFromCollection(collection, martin.getUri().toString());
        assertEquals(1, martin.getRelatedArticles().size());
        assertEquals("/places/bedfordshire", martin.getRelatedArticles().get(0).getUri().toString());
    }

    @Test
    public void shouldChangeSubReferencesInFileOnMoveContent() throws URISyntaxException, IOException, ZebedeeException {
        // Given
        // an item of content that references a sub page
        martin.getRelatedArticles().add(new Link(new URI("/places/bedford/central")));
        savePages();

        // When
        // we run the move on the upper
        collection.moveContent(session, "/places/bedford", "/places/london");

        // Then
        // the link should be updated on the lower level
        martin = (Article) readPageFromCollection(collection, martin.getUri().toString());
        assertEquals(1, martin.getRelatedArticles().size());
        assertEquals("/places/london/central", martin.getRelatedArticles().get(0).getUri().toString());
    }

    Article createArticle(String uri, String title) throws URISyntaxException {
        Article article = new Article();
        article.setDescription(new PageDescription());
        article.setRelatedArticles(new ArrayList<Link>());
        article.setSections(new ArrayList<MarkdownSection>());
        article.setUri(new URI(uri));
        article.getDescription().setTitle(title);
        return article;
    }

    void writePageToContent(Content content, Page page) throws IOException {
        Path path = content.toPath(page.getUri().toString()).resolve("data.json");
        path.toFile().getParentFile().mkdirs();

        Files.write(path, ContentUtil.serialise(page).getBytes());
    }

    void savePages() throws IOException {
        writePageToContent(collection.inProgress, martin);
        writePageToContent(collection.inProgress, bedford);
        writePageToContent(collection.inProgress, bedfordshire);
    }

    void reloadPages() throws IOException {
        martin = (Article) readPageFromCollection(collection, martin.getUri().toString());
        bedford = (Article) readPageFromCollection(collection, bedford.getUri().toString());
        bedfordshire = (Article) readPageFromCollection(collection, bedfordshire.getUri().toString());
    }

    Page readPageFromCollection(Collection collection, String uri) throws IOException {
        Path path = collection.find(uri).resolve("data.json");
        Page page = null;
        try (InputStream stream = Files.newInputStream(path)) {
            page = ContentUtil.deserialiseContent(stream);
        }
        return page;
    }
}