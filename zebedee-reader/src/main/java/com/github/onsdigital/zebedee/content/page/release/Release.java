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
    private List<Link> relatedMethodologyArticle;
    private List<Link> links;

    private List<ReleaseDateChange> dateChanges;

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
        ArrayList<String> cancellationNotice = new ArrayList<>();
        cancellationNotice.add("Due to ....");
        release.getDescription().setCancellationNotice(cancellationNotice);
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

        ArrayList<String> markdowns = new ArrayList<>();
        markdowns.add("The phrase 'Pre-release Access' refers to the practice whereby official statistics in their final form, and any\n" +
                "accompanying written commentary, are made available to an eligible person in advance of their publication. The rules and principles which govern pre-release access are featured within the Pre-release Access to Official Statistics Order 2008.\n" +
                "\n" +
                "Besides ONS staff, the following persons are given pre-release access by the period indicated before release.\n" +
                "\n" +
                "- Prime Minister 10 Downing Street 24 hrs\n" +
                "- PS to Prime Minister 10 Downing Street 24 hrs\n" +
                "- PPS to Prime Minister 10 Downing Street 24 hrs\n" +
                "- Parliamentary Clerk 10 Downing Street 24 hrs\n" +
                "- Senior Policy Advisor 10 Downing Street 24 hrs\n" +
                "- Deputy Prime Minister Deputy Prime Minister's Office 24 hrs\n" +
                "- Principal Private Secretary to DPM Deputy Prime Minister's Office 24 hrs\n" +
                "- Private Secretary to DPM Deputy Prime Minister's Office 24 hrs\n" +
                "- Chief Mouser to the Cabinet Office 10 Downing Street 24 hrs\n" +
                "- Cabinet Secretary Deputy Prime Minister's Office 24 hrs");
        release.setMarkdowns(markdowns);

        ArrayList changes = new ArrayList();
        ReleaseDateChange dateChange = new ReleaseDateChange();
        dateChange.setPreviousDate(new Date());
        dateChange.setChangeNotice("Due to....");
        changes.add(dateChange);
        release.setDateChanges(changes);

        release.getDescription().setPublished(false);

        System.out.println(ContentUtil.serialise(release));

    }

    @Override
    public PageType getType() {
        return PageType.release;
    }

    public List<String> getMarkdown() {
        return markdown;
    }

    public void setMarkdowns(List<String> markdown) {
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

    public List<Link> getRelatedMethodologyArticle() {
        return relatedMethodologyArticle;
    }

    public void setRelatedMethodologyArticle(List<Link> relatedMethodologyArticle) {
        this.relatedMethodologyArticle = relatedMethodologyArticle;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }
}
