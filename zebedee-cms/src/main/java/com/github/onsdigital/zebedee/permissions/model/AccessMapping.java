package com.github.onsdigital.zebedee.permissions.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashMap;
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

    /**
     * Content owners can be assigned access to collections via one or more teams.
     * <p>NB: This is a map of collection ID to the set of team IDs that have access to the collection.</p>
     */
    private Map<String, Set<Integer>> collections;

    /**
     * Create a new AccessMapping.
     */
    public AccessMapping() {
        this.administrators = new HashSet<>();
        this.digitalPublishingTeam = new HashSet<>();
        this.collections = new HashMap<>();
    }

    public Set<String> getAdministrators() {
        return administrators;
    }

    public Set<String> getDigitalPublishingTeam() {
        return digitalPublishingTeam;
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

    public void setCollections(Map<String, Set<Integer>> collections) {
        this.collections = collections;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        AccessMapping that = (AccessMapping) obj;
        return new EqualsBuilder()
                .append(getAdministrators(), that.getAdministrators())
                .append(getDigitalPublishingTeam(), that.getDigitalPublishingTeam())
                .append(getCollections(), that.getCollections())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getAdministrators())
                .append(getDigitalPublishingTeam())
                .append(getCollections())
                .toHashCode();
    }
}
