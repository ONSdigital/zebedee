package com.github.onsdigital.zebedee.permissions.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by david on 17/03/2015.
 */
public class AccessMapping {

    /**
     * System administrators - this is for administrative permission only.
     */
    private Set<String> administrators;

    /**
     * Members of the Digital Publishing Team can view and edit everything.
     */
    private Set<String> digitalPublishingTeam;


    private Set<String> dataVisualisationPublishers;

    /**
     * Content owners can be assigned access to collections via one or more teams.
     * <p>NB: This is a map of collection ID to the set of team IDs that have access to the collection.</p>
     */
    public Map<String, Set<Integer>> collections;

    public Set<String> getAdministrators() {
        return administrators;
    }

    public Set<String> getDigitalPublishingTeam() {
        return digitalPublishingTeam;
    }

    public Set<String> getDataVisualisationPublishers() {
        return dataVisualisationPublishers;
    }

    public Map<String, Set<Integer>> getCollections() {
        return collections;
    }

    public void setAdministrators(Set<String> administrators) {
        this.administrators = administrators;
    }

    public void setDigitalPublishingTeam(Set<String> digitalPublishingTeam) {
        this.digitalPublishingTeam = digitalPublishingTeam;
    }

    public void setDataVisualisationPublishers(Set<String> dataVisualisationPublishers) {
        this.dataVisualisationPublishers = dataVisualisationPublishers;
    }

    public void setCollections(Map<String, Set<Integer>> collections) {
        this.collections = collections;
    }
}
