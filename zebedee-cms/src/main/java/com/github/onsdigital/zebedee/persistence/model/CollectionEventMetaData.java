package com.github.onsdigital.zebedee.persistence.model;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.TableModifications;
import com.github.onsdigital.zebedee.content.page.visualisation.Visualisation;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides functionality for created the necessary meta data items for each event history scenario.
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
    static final String TABLE_XLS_MODIFIED = "updatedXLSFile";
    static final String TABLE_JSON_MODIFIED = "updateJSONFile";
    static final String TABLE_HTML_MODIFIED = "updatedHTMLFile";
    static final String FILE_SOURCE = "fileSource";
    static final String FILE_DEST = "fileDestination";
    static final String FROM = "from";
    static final String TO = "to";
    static final String ZIP_NAME = "zipTitle";
    static final String INDEX_PAGE = "indexPage";
    static final String URI = "uri";
    static final String TABLE_MODIFICATIONS = "tableModifications";
    static final String DELETE_MARKER_ADDED = "deleteMarkerAdded";
    static final String DELETE_MARKER_REMOVED = "deleteMarkerRemoved";
    static final String DELETE_ROOT = "deleteRoot";
    static final String DELETE_ROOT_REMOVED = "deleteRootRemoved";

    private static final String HTML = ".html";
    private static final String XLS = ".xls";
    private static final String JSON = ".json";
    private static final String FILE_INDEX_KEY = "file[{0}]";

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
        List<CollectionEventMetaData> list = new ArrayList<>();
        if (team != null && StringUtils.isNotEmpty(team.getName())) {
            list.add(new CollectionEventMetaData(TEAM_REMOVED_KEY, team.getName()));
        }

        if (collectionDescription != null && session != null) {
            list.add(new CollectionEventMetaData(VIEWER_TEAMS_KEY, viewerTeamsAsStr(collectionDescription,
                    session)));
        }
        return toArray(list);
    }

    /**
     * Create a {@link CollectionEventMetaData} for viewer team added event.
     */
    public static CollectionEventMetaData[] teamAdded(CollectionDescription collectionDescription, Session session,
                                                      Team team) throws IOException, ZebedeeException {

        List<CollectionEventMetaData> list = new ArrayList<>();
        if (team != null && StringUtils.isNotEmpty(team.getName())) {
            list.add(new CollectionEventMetaData(TEAM_ADDED_KEY, team.getName()));
        }

        if (collectionDescription != null && session != null) {
            list.add(new CollectionEventMetaData(VIEWER_TEAMS_KEY, viewerTeamsAsStr(collectionDescription,
                    session)));
        }
        return toArray(list);
    }

    private static String viewerTeamsAsStr(CollectionDescription collectionDescription, Session session)
            throws
            IOException, ZebedeeException {
        Set<Integer> teams = Root.zebedee.getPermissionsService().listViewerTeams(collectionDescription, session);
        Iterator<Team> iterator = Root.zebedee.getTeamsService().resolveTeams(teams).iterator();
        StringBuilder teamsListStr = new StringBuilder();

        while (iterator.hasNext()) {
            teamsListStr.append(iterator.next().getName()).append(iterator.hasNext() ? ", " : "");
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
        List<CollectionEventMetaData> list = new ArrayList<>();

        if (updatedCollectionDescription != null && updatedCollectionDescription.getType() != null) {
            CollectionType previousType = updatedCollectionDescription.getType().equals(CollectionType.manual)
                    ? CollectionType.scheduled : CollectionType.manual;

            CollectionType updatedType = updatedCollectionDescription.getType();

            list.add(new CollectionEventMetaData(PREVIOUS_TYPE, previousType.name()));
            list.add(new CollectionEventMetaData(UPDATED_TYPE, updatedType.name()));
        }
        return toArray(list);
    }

    /**
     * Create a {@link CollectionEventMetaData} for {@link com.github.onsdigital.zebedee.model.Collection} rescheduled.
     */
    public static CollectionEventMetaData[] reschedule(Date originalDate, Date newDate) {
        List<CollectionEventMetaData> list = new ArrayList<>();
        if (originalDate != null) {
            list.add(new CollectionEventMetaData(PREVIOUS_PUBLISH_DATE, originalDate.toString()));
        }
        if (newDate != null) {
            list.add(new CollectionEventMetaData(PUBLISH_DATE, newDate.toString()));
        }
        return toArray(list);
    }

    /**
     * Create a {@link CollectionEventMetaData} for {@link com.github.onsdigital.zebedee.model.Collection} created.
     */
    public static CollectionEventMetaData[] collectionCreated(CollectionDescription description) {
        List<CollectionEventMetaData> list = new ArrayList<>();
        if (description == null) {
            return null;
        }

        if (description.getType() != null) {
            list.add(new CollectionEventMetaData(PUBLISH_TYPE, description.getType().toString()));

            if (description.getType().equals(CollectionType.scheduled)) {
                list.add(new CollectionEventMetaData(PUBLISH_DATE, description.getPublishDate().toString()));
            }
        }
        return toArray(list);
    }

    /**
     * Create a {@link CollectionEventMetaData} for table modified.
     */
    public static CollectionEventMetaData[] tableModified(String uri) {
        List<CollectionEventMetaData> list = new ArrayList<>();

        if (StringUtils.isNotEmpty(uri)) {
            list.add((new CollectionEventMetaData(TABLE_XLS_MODIFIED, uri + XLS)));
            list.add((new CollectionEventMetaData(TABLE_JSON_MODIFIED, uri + JSON)));
            list.add((new CollectionEventMetaData(TABLE_HTML_MODIFIED, uri + HTML)));
        }
        return toArray(list);
    }

    /**
     * Create a {@link CollectionEventMetaData} for table created.
     */
    public static CollectionEventMetaData[] tableCreated(String uri, TableModifications tableModifications) {
        List<CollectionEventMetaData> list = new ArrayList<>();

        if (StringUtils.isNotEmpty(uri)) {
            list.add(new CollectionEventMetaData(URI, uri));
        }
        if (tableModifications != null) {
            list.add(new CollectionEventMetaData(TABLE_MODIFICATIONS, tableModifications.summary()));
        }
        return toArray(list);
    }

    public static CollectionEventMetaData[] contentReviewed(Path source, Path dest) {
        List<CollectionEventMetaData> list = new ArrayList<>();
        if (source != null) {
            list.add(new CollectionEventMetaData(FILE_SOURCE, source.toString()));
        }

        if (dest != null) {
            list.add(new CollectionEventMetaData(FILE_DEST, dest.toString()));
        }
        return toArray(list);
    }

    public static CollectionEventMetaData[] contentRenamed(String uri, String toUri) {
        List<CollectionEventMetaData> list = new ArrayList<>();
        if (StringUtils.isNotEmpty(uri)) {
            list.add(new CollectionEventMetaData(FROM, uri));
        }

        if (StringUtils.isNotEmpty(toUri)) {
            list.add(new CollectionEventMetaData(TO, toUri));
        }
        return toArray(list);
    }

    public static CollectionEventMetaData[] contentMoved(String uri, String toUri) {
        return contentRenamed(uri, toUri);
    }

    public static CollectionEventMetaData[] dataVisZipUnpacked(Visualisation visualisation) {
        List<CollectionEventMetaData> list = new ArrayList<>();
        if (visualisation != null) {
            list.add(new CollectionEventMetaData(ZIP_NAME, visualisation.zipTitle));

            if (visualisation.getFilenames() != null && !visualisation.getFilenames().isEmpty()) {
                int i = 0;

                for (String zipFile : visualisation.getFilenames()) {
                    Path filePath = Paths.get(zipFile);
                    list.add(i, new CollectionEventMetaData(fileIndex(i), filePath.toString()));
                    i++;
                }
                list.add(new CollectionEventMetaData(INDEX_PAGE, visualisation.getIndexPage()));
            }
            return toArray(list);
        }
        return null;
    }

    public static CollectionEventMetaData[] deleteMarkerAdded(String deleteRoot, List<String> uris) {
        List<CollectionEventMetaData> markedDelete = new ArrayList<>();
        markedDelete.add(create(DELETE_ROOT, deleteRoot));
        markedDelete.addAll(uris.stream()
                .map(uri -> new CollectionEventMetaData(DELETE_MARKER_ADDED, uri))
                .collect(Collectors.toList()));
        return toArray(markedDelete);
    }

    public static CollectionEventMetaData[] deleteMarkerRemoved(String deleteRoot, List<String> uris) {
        List<CollectionEventMetaData> markedDelete = new ArrayList<>();
        markedDelete.add(create(DELETE_ROOT_REMOVED, deleteRoot));
        markedDelete.addAll(uris.stream()
                .map(uri -> new CollectionEventMetaData(DELETE_MARKER_REMOVED, uri))
                .collect(Collectors.toList()));
        return toArray(markedDelete);
    }

    private static String fileIndex(int index) {
        return MessageFormat.format(FILE_INDEX_KEY, index);
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
