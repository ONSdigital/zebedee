package com.github.onsdigital.zebedee.persistence.model;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.Team;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by dave on 6/3/16.
 */
public class CollectionEventMetaData {

    static final String TEAM_REMOVED_KEY = "teamRemoved";
    static final String TEAM_ADDED_KEY = "teamAdded";
    static final String VIEWER_TEAMS_KEY = "currentViewerTeams";
    static final String PREVIOUS_NAME = "previousName";
    static final String PREVIOUS_TYPE = "previousType";
    static final String UPDATED_TYPE = "updatedType";
    static final String PUBLISH_DATE = "publishDate";
    static final String PREVIOUS_PUBLISH_DATE = "previousPublishDate";
    static final String PUBLISH_TYPE = "publishType";
    static final String COLLECTION_OWNER = "collectionOwner";

    private final String key;
    private final String value;

    /**
     * Construct a new collection event meta data object.
     *
     * @param key
     * @param value
     */
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

    public static CollectionEventMetaData create(String key, String value) {
        return new CollectionEventMetaData(key, value);
    }

    /**
     * Create a {@link CollectionEventMetaData} for viewer team removed event.
     */
    public static CollectionEventMetaData[] teamRemoved(CollectionDescription collectionDescription,
                                                        Session session, Team team) throws IOException, ZebedeeException {
        List<CollectionEventMetaData> metaDataList = new ArrayList<>();
        if (team != null && StringUtils.isNotEmpty(team.name)) {
            metaDataList.add(new CollectionEventMetaData(TEAM_REMOVED_KEY, team.name));
        }

        if (collectionDescription != null && session != null) {
            metaDataList.add(new CollectionEventMetaData(VIEWER_TEAMS_KEY, viewerTeamsAsStr(collectionDescription,
                    session)));
        }
        return toArray(metaDataList);
    }

    /**
     * Create a {@link CollectionEventMetaData} for viewer team added event.
     */
    public static CollectionEventMetaData[] teamAdded(CollectionDescription collectionDescription, Session session,
                                                      Team team) throws IOException, ZebedeeException {

        List<CollectionEventMetaData> metaDataList = new ArrayList<>();
        if (team != null && StringUtils.isNotEmpty(team.name)) {
            metaDataList.add(new CollectionEventMetaData(TEAM_ADDED_KEY, team.name));
        }

        if (collectionDescription != null && session != null) {
            metaDataList.add(new CollectionEventMetaData(VIEWER_TEAMS_KEY, viewerTeamsAsStr(collectionDescription,
                    session)));
        }
        return toArray(metaDataList);
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

    /**
     * Create a {@link CollectionEventMetaData} for collection renamed.
     */
    public static CollectionEventMetaData renamed(String previousName) {
        if (StringUtils.isEmpty(previousName)) {
            return null;
        }
        return new CollectionEventMetaData(PREVIOUS_NAME, previousName);
    }

    /**
     * Create a {@link CollectionEventMetaData} for {@link CollectionType} changed.
     */
    public static CollectionEventMetaData[] typeChanged(CollectionDescription updatedCollectionDescription) {
        List<CollectionEventMetaData> metaDataList = new ArrayList<>();

        if (updatedCollectionDescription != null && updatedCollectionDescription.type != null) {
            CollectionType previousType = updatedCollectionDescription.type.equals(CollectionType.manual)
                    ? CollectionType.scheduled : CollectionType.manual;

            CollectionType updatedType = updatedCollectionDescription.type;

            metaDataList.add(new CollectionEventMetaData(PREVIOUS_TYPE, previousType.name()));
            metaDataList.add(new CollectionEventMetaData(UPDATED_TYPE, updatedType.name()));
        }
        return toArray(metaDataList);
    }

    /**
     * Create a {@link CollectionEventMetaData} for {@link com.github.onsdigital.zebedee.model.Collection} rescheduled.
     */
    public static CollectionEventMetaData[] reschedule(Date originalDate, Date newDate) {
        List<CollectionEventMetaData> metaDataList = new ArrayList<>();
        if (originalDate != null) {
            metaDataList.add(new CollectionEventMetaData(PREVIOUS_PUBLISH_DATE, originalDate.toString()));
        }
        if (newDate != null) {
            metaDataList.add(new CollectionEventMetaData(PUBLISH_DATE, newDate.toString()));
        }
        return toArray(metaDataList);
    }

    /**
     * Create a {@link CollectionEventMetaData} for {@link com.github.onsdigital.zebedee.model.Collection} created.
     */
    public static CollectionEventMetaData[] collectionCreated(CollectionDescription description) {
        List<CollectionEventMetaData> metaDataList = new ArrayList<>();
        if (description == null) {
            return null;
        }

        if (description.type != null) {
            metaDataList.add(new CollectionEventMetaData(PUBLISH_TYPE, description.type.toString()));

            if (description.type.equals(CollectionType.scheduled)) {
                metaDataList.add(new CollectionEventMetaData(PUBLISH_DATE, description.publishDate.toString()));
            }
        }

        if (description.collectionOwner != null) {
            metaDataList.add(new CollectionEventMetaData(COLLECTION_OWNER, description.getCollectionOwner().name()));
        }
        return toArray(metaDataList);
    }

    private static CollectionEventMetaData[] toArray(List<CollectionEventMetaData> metaDataValues) {
        return metaDataValues.toArray(new CollectionEventMetaData[metaDataValues.size()]);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
