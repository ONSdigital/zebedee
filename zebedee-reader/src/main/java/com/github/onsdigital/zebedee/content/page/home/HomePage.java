package com.github.onsdigital.zebedee.content.page.home;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.taxonomy.base.TaxonomyNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bren on 04/06/15.
 * <p>
 * Object mapping for homepage of the website
 * <p>
 * HomePage is considered as the root of Taxonomy. It also contains links and references to non-statistics contents (Methodology, Release, About Us pages , etc.)
 */
public class HomePage extends TaxonomyNode {

    private List<HomeContentItem> featuredContent;

    private List<HomeContentItem> aroundONS;
    
    private EmergencyBanner emergencyBanner;

    private String serviceMessage;

    public HomePage() {
        this.featuredContent = new ArrayList<HomeContentItem>();
        this.aroundONS = new ArrayList<HomeContentItem>();
    }

    @Override
    public PageType getType() {
        return PageType.home_page;
    }

    public List<HomeContentItem> getFeaturedContent() {
        return featuredContent;
    }

    public void setFeaturedContent(List<HomeContentItem> featuredContent) {
        this.featuredContent = featuredContent;
    }

    public List<HomeContentItem> getAroundONS() {
        return aroundONS;
    }

    public void setAroundONS(List<HomeContentItem> aroundONS) {
        this.aroundONS = aroundONS;
    }

    public String getServiceMessage() {
        return serviceMessage;
    }

    public void setServiceMessage(String serviceMessage) {
        this.serviceMessage = serviceMessage;
    }
    
    public EmergencyBanner getEmergencyBanner() {
        return emergencyBanner;
    }
    
    public void setEmergencyBanner(EmergencyBanner emergencyBanner) {
        this.emergencyBanner = emergencyBanner;
    }
}
