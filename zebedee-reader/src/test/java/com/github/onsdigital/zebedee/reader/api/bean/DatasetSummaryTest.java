package com.github.onsdigital.zebedee.reader.api.bean;

import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import dp.api.dataset.model.Dataset;
import dp.api.dataset.model.DatasetLinks;
import dp.api.dataset.model.Link;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetSummaryTest {

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDatasetLandingPageToDatasetSummary() throws URISyntaxException {
        PageDescription desc = new PageDescription();
        desc.setTitle("Hells Bells");
        desc.setSummary(
                "I'm a rolling thunder, a pouring rain\n" +
                "I'm comin' on like a hurricane\n" +
                "My lightning's flashing across the sky\n" +
                "You're only young but you're gonna die"
        );

        DatasetLandingPage dlp = new DatasetLandingPage();
        dlp.setUri(new URI("https://www.acdc.com/"));
        dlp.setDescription(desc);

        DatasetSummary summary = new DatasetSummary(dlp);

        assertThat(summary.getSummary(), equalTo(dlp.getDescription().getSummary()));
        assertThat(summary.getUri(), equalTo(dlp.getUri().toString()));
        assertThat(summary.getTitle(), equalTo(dlp.getDescription().getTitle()));
    }

    @Test
    public void testAPIDatasetToDatasetSummary() {
        Link link = new Link();
        link.setHref("https://www.metallica.com/");

        DatasetLinks links = new DatasetLinks();
        links.setSelf(link);

        Dataset dataset = new Dataset();
        dataset.setTitle("For Whom the Bell Tolls");
        dataset.setLinks(links);
        dataset.setDescription(
                "Make his fight on the hill in the early day\n" +
                "Constant chill deep inside\n" +
                "Shouting gun, on they run through the endless grey\n" +
                "On the fight, for they are right, yes, by who's to say?\n" +
                "For a hill men would kill, why? They do not know\n" +
                "Stiffened wounds test there their pride\n" +
                "Men of five, still alive through the raging glow\n" +
                "Gone insane from the pain that they surely know\n" +
                "For whom the bell tolls\n" +
                "Time marches on\n" +
                "For whom the bell tolls"
        );

        DatasetSummary summary = new DatasetSummary(dataset);

        assertThat(summary.getTitle(), equalTo(dataset.getTitle()));
        assertThat(summary.getUri(), equalTo(dataset.getLinks().getSelf().getHref()));
        assertThat(summary.getSummary(), equalTo(dataset.getDescription()));
    }
}
