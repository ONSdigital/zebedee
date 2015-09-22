package com.github.onsdigital.zebedee.content.page.release;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.Contact;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.content.util.ContentUtil;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class Release extends Page{

    private List<String> markdown;
    private List<Link> relatedDocuments;
    private List<Link> relatedDatasets;
    private List<Link> relatedMethodology;

    private List<ReleaseDateChange> dateChanges;

    @Override
    public PageType getType() {
        return PageType.release;
    }

    public List<String> getMarkdown() {
        return markdown;
    }

    public void setMarkdown(List<String> markdown) {
        this.markdown = markdown;
    }

    public List<ReleaseDateChange> getDateChanges() {
        return dateChanges;
    }

    public void setDateChanges(List<ReleaseDateChange> dateChanges) {
        this.dateChanges = dateChanges;
    }

    public List<Link> getRelatedDocuments() {
        return relatedDocuments;
    }

    public void setRelatedDocuments(List<Link> relatedDocuments) {
        this.relatedDocuments = relatedDocuments;
    }

    public List<Link> getRelatedDatasets() {
        return relatedDatasets;
    }

    public void setRelatedDatasets(List<Link> relatedDatasets) {
        this.relatedDatasets = relatedDatasets;
    }

    public List<Link> getRelatedMethodology() {
        return relatedMethodology;
    }

    public void setRelatedMethodology(List<Link> relatedMethodology) {
        this.relatedMethodology = relatedMethodology;
    }

    public static void main(String[] args) {
        Release release = new Release();
        release.setDescription(new PageDescription());
        release.getDescription().setTitle("Consumer Price Inflation");
        release.getDescription().setEdition("January 2015");
        release.getDescription().setReleaseDate(new Date());
        release.getDescription().setNextRelease("To be announced");
        release.getDescription().setSummary("Inflation measures the rate of increase in prices for goods and services using price indices. The rate of inflation used in the UK\n" +
                "government’s inflation target is measured using the Consumer Prices Index (CPI). One way to understand the CPI is to think of\n" +
                "a very large shopping basket containing a large selection of the goods and services bought by households, the CPI estimates\n" +
                "changes to the total cost of this basket.");
        release.getDescription().setCancelled(true);
//        release.getDescription().setCancellationNotice("Due to ....");
        release.getDescription().setFinalised(true);

        ArrayList linkList = new ArrayList();
        linkList.add(new Link(URI.create("/economy/inflationandpriceindices/bulletins/consumerpriceinflation/2015-09-15")));

        ArrayList dataSetlist = new ArrayList();
        dataSetlist.add(new Link(URI.create("/economy/inflationandpriceindices/datasets/consumerpriceindices")));

        release.setRelatedDocuments(linkList);
        release.setRelatedDatasets(dataSetlist);

        Contact contact = new Contact();
        contact.setName("Kat Pegler");
        contact.setEmail("ppi@ons.gsi.gov.uk");
        contact.setTelephone("+44 (0) 1633 456468");
        release.getDescription().setContact(contact);
        release.getDescription().setNationalStatistic(true);



        ArrayList changes = new ArrayList();
        ReleaseDateChange dateChange = new ReleaseDateChange();
        dateChange.setPreviousDate(new Date());
        dateChange.setChangeNotice("Due to....");
        changes.add(dateChange);
        release.setDateChanges(changes);

        release.getDescription().setPublished(false);

        System.out.println(ContentUtil.serialise(release));

    }
}
