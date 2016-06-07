package com.github.onsdigital.zebedee.persistence.model;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.Team;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by dave on 6/3/16.
 */
public class CollectionEventMetaData {

    private static final String TEAM_REMOVED_KEY = "teamRemoved";
    private static final String TEAM_ADDED_KEY = "teamAdded";
    private static final String VIEWER_TEAMS_KEY = "currentViewerTeams";
    private static final String PREVIOUS_NAME = "previousName";
    private static final String PREVIOUS_TYPE = "previousType";
    private static final String UPDATED_TYPE = "updatedType";
    private static final String PUBLISH_DATE = "publishDate";
    private static final String PREVIOUS_PUBLISH_DATE = "previousPublishDate";
    private static final String PUBLISH_TYPE = "publishType";
    private static final String COLLECTION_OWNER = "collectionOwner";

    private final String key;
    private final String value;

    private CollectionEventMetaData(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public static CollectionEventMetaData[] teamRemoved(CollectionDescription collectionDescription,
                                                        Session session, Team team) throws IOException, ZebedeeException {
        return toArray(
                new CollectionEventMetaData(TEAM_REMOVED_KEY, team.name),
                new CollectionEventMetaData(VIEWER_TEAMS_KEY, viewerTeamsAsStr(collectionDescription, session))
        );
    }

    public static CollectionEventMetaData[] teamAdded(CollectionDescription collectionDescription, Session session,
                                                      Team team) throws IOException, ZebedeeException {
        return toArray(
                new CollectionEventMetaData(TEAM_ADDED_KEY, team.name),
                new CollectionEventMetaData(VIEWER_TEAMS_KEY, viewerTeamsAsStr(collectionDescription, session))
        );
    }

    private static String viewerTeamsAsStr(CollectionDescription collectionDescription, Session session)
            throws
            IOException, ZebedeeException {
        Set<Integer> teams = Root.zebedee.permissions.listViewerTeams(collectionDescription, session);
        Iterator<Team> iterator = Root.zebedee.teams.resolveTeams(teams).iterator();
        StringBuilder teamsListStr = new StringBuilder();

        while (iterator.hasNext()) {
            teamsListStr.append(iterator.next().name).append(iterator.hasNext() ? ", " : "");
        }
        return teamsListStr.toString();
    }

    public static CollectionEventMetaData renamed(String previousName) {
        return new CollectionEventMetaData(PREVIOUS_NAME, previousName);
    }

    public static CollectionEventMetaData[] typeChanged(CollectionDescription updatedCollectionDescription) {
        CollectionType previousType = updatedCollectionDescription.type.equals(CollectionType.manual)
                ? CollectionType.scheduled : CollectionType.manual;

        CollectionType updatedType = updatedCollectionDescription.type;

        return toArray(
                new CollectionEventMetaData(PREVIOUS_TYPE, previousType.name()),
                new CollectionEventMetaData(UPDATED_TYPE, updatedType.name())
        );
    }

    public static CollectionEventMetaData[] reschedule(Date originalDate, Date newDate) {
        return toArray(
                new CollectionEventMetaData(PREVIOUS_PUBLISH_DATE, originalDate.toString()),
                new CollectionEventMetaData(PUBLISH_DATE, newDate.toString())
        );
    }

    public static CollectionEventMetaData[] collectionCreated(CollectionDescription description) {
        if (description.type.equals(CollectionType.manual)) {
            return toArray(
                    new CollectionEventMetaData(PUBLISH_TYPE, description.type.toString()),
                    new CollectionEventMetaData(COLLECTION_OWNER, description.collectionOwner.name())
            );
        }

        return toArray(
                new CollectionEventMetaData(PUBLISH_DATE, description.publishDate.toString()),
                new CollectionEventMetaData(PUBLISH_TYPE, description.type.toString()),
                new CollectionEventMetaData(COLLECTION_OWNER, description.collectionOwner.name())
        );
    }

    private static CollectionEventMetaData[] toArray(CollectionEventMetaData... metaData) {
        return metaData;
    }
}
