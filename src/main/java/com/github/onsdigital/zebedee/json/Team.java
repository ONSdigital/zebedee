package com.github.onsdigital.zebedee.json;

import java.util.Set;

/**
 * Created by david on 21/04/2015.
 */
public class Team {
    public int id;
    public String name;
    public Set<String> members;

    @Override
    public String toString() {
        return name + " (" + id + ") ";
    }
}
