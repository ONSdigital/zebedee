package com.github.onsdigital.zebedee.json;

import java.util.Map;
import java.util.Set;

/**
 * Created by david on 17/03/2015.
 */
public class AccessMapping {

    /**
     * System administrators - this is for administrative permission only.
     */
    public Set<String> administrators;

    /**
     * Members of the Digital Publishing Team can view and edit everything.
     */
    public Set<String> digitalPublishingTeam;

    /**
     * Content owners can be assigned access to collections via one or more teams.
     */
    public Map<String, Set<Integer>> collections;
}
