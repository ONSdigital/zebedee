package com.github.onsdigital.zebedee.audit;

import com.github.onsdigital.zebedee.model.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Provides functionality for logging key events in Zebedee. {@link Event#parameters()} returns a {@link ParameterBuilder}
 * which allows parameters to be added to the log message, alternatively you can invoke {@link Event#logWithoutParameters()}
 * to log an event without providing any parameters.
 */
public class Audit {

    public enum Event {

        ZEBEDEE_STARTUP("Zebedee start up successful."),

        /**
         * User events
         */
        LOGIN_SUCCESS("User Login"),

        LOGIN_AUTHENTICATION_FAILURE("User login authentication failure"),

        LOGIN_PASSWORD_CHANGE_REQUIRED("User login successful password change required"),

        PASSWORD_CHANGED_SUCCESS("User password change successfully"),

        PASSWORD_CHANGED_FAILURE("User password change failed"),

        USER_CREATED("User created"),

        USER_UPDATED("User updated"),

        USER_DELETED("User deleted"),

        /**
         * Permission events.
         */
        ADMIN_PERMISSION_ADDED("Administrator permission added"),

        ADMIN_PERMISSION_REMOVED("Administrator permission removed"),

        PUBLISHER_PERMISSION_ADDED("Publisher permission added"),

        PUBLISHER_PERMISSION_REMOVED("Publisher permission removed"),


        /**
         * Collections events
         */
        COLLECTION_APPROVED("Collection approved"),

        COLLECTION_PUBLISHED("Collection published"),

        COLLECTION_PUBLISH_UNSUCCESSFUL("Collection publish unsucessful"),

        COLLECTION_CREATED("Collection created"),

        COLLECTION_UPDATED("Collection updated"),

        COLLECTION_DELETED("Collection deleted"),

        COLLECTION_MOVED_TO_REVIEWED("Collection moved to review"),

        COLLECTION_TABLE_CREATED("Collection Table created"),

        COLLECTION_TABLE_METADATA_MODIFIED("Collection table metadata modified"),

        COLLECTION_TRANSFERRED("Collection transferred"),

        COLLECTION_UNLOCKED("Collection unlocked"),

        COLLECTION_VERSION_CREATED("Collection version created"),

        COLLECTION_VERSION_DELETED("Collection version deleted"),

        /**
         * Collection Content events.
         */
        CONTENT_DELETED("Collection content deleted"),

        CONTENT_OVERWRITTEN("Collection content overwritten"),

        CONTENT_SAVED("Collection content saved"),

        CONTENT_MOVED("Collection content moved"),

        CONTENT_RENAMED("Collection content renamed"),

        /**
         * Team events.
         */
        TEAM_MEMBER_ADDED("Team member added"),

        TEAM_MEMBER_REMOVED("Team member removed"),

        TEAM_CREATED("Team created"),

        TEAM_DELETED("Team deleted"),

        /**
         * CSDB events.
         */
        CSDB_NEW_FILE_NOTIFICATION("CSDB file notification received"),

        /**
         * Content Delete Markers
         */
        DELETE_MARKER_ADDED("Delete Marker added to content."),

        DELETE_MARKER_REMOVED("Delete marker removed from content.");

        /**
         * The event description.
         */
        private final String eventDesc;

        /**
         * Construct a new event.
         *
         * @param eventDesc description of the event.
         */
        Event(String eventDesc) {
            this.eventDesc = eventDesc;
        }

        /**
         * @return {@link ParameterBuilder} for building log event parameters for a given event.
         */
        public ParameterBuilder parameters() {
            return new ParameterBuilder(this);
        }

        /**
         * Log this event without any parameters.
         */
        public void logWithoutParameters() {
            new ParameterBuilder(this).log();
        }
    }

    /**
     * Builder object providing a convenient interface for adding parameters to the log message.
     */
    public static class ParameterBuilder {

        private static final Logger LOG = LoggerFactory.getLogger("com.github.onsdigital.zebedee.audit");
        private static final String DEFAULT_REMOTE_IP = "localhost";


        private Event event;
        private List<Object> parameters = new ArrayList<>();

        private ParameterBuilder(Event event) {
            this.event = event;
        }

        private String getHostname(HttpServletRequest request) {
            String remoteIP = DEFAULT_REMOTE_IP;

            if (request != null) {
                remoteIP = request.getHeader("X-Forwarded-For");
                if (remoteIP == null || remoteIP.length() == 0) {
                    remoteIP = request.getRemoteAddr() != null ? request.getRemoteAddr() : DEFAULT_REMOTE_IP;
                }
            }
            return remoteIP;
        }

        /**
         * Adds the host request originated from.
         */
        public ParameterBuilder host(HttpServletRequest request) {
            parameters.add("Host: " + getHostname(request));
            return this;
        }

        /**
         * Adds the user who actioned the event.
         */
        public ParameterBuilder user(String userEmail) {
            parameters.add("User: " + userEmail);
            return this;
        }

        /**
         * Adds the user who actioned the event.
         */
        public ParameterBuilder actionedBy(String userEmail) {
            parameters.add("Actioned by: " + userEmail);
            return this;
        }

        /**
         * Adds the user who actioned the event and the user who is affected by the action.
         */
        public ParameterBuilder actionedByEffecting(String byUserEmail, String forUserEmail) {
            parameters.add("Action by: " + byUserEmail + " | Affecting: " + forUserEmail);
            return this;
        }

        /**
         * The collection affected.
         */
        public ParameterBuilder collection(Collection collection) {
            parameters.add("Collection: " + collection.path);
            return this;
        }

        /**
         * The collection affected.
         */
        public ParameterBuilder collection(String collection) {
            parameters.add("Collection: " + collection);
            return this;
        }

        /**
         * The content affected.
         */
        public ParameterBuilder content(String content) {
            parameters.add("Content: " + content);
            return this;
        }

        /**
         * The team affected.
         */
        public ParameterBuilder team(String teamName) {
            parameters.add("Team: " + teamName);
            return this;
        }

        /**
         * The team member affected.
         */
        public ParameterBuilder teamMember(String teamMember) {
            parameters.add("Team Member: " + teamMember);
            return this;
        }

        /**
         * From x to y.
         */
        public ParameterBuilder fromTo(String from, String to) {
            parameters.add("from: " + from + " | to: " + to);
            return this;
        }

        public ParameterBuilder version(String version) {
            parameters.add("version: " + version);
            return this;
        }

        public ParameterBuilder fileName(String fileName) {
            parameters.add("fileName: " + fileName);
            return this;
        }

        /**
         * Log the event.
         */
        public void log() {
            StringBuilder message = new StringBuilder(event.eventDesc + " ");

            if (!parameters.isEmpty()) {
                Iterator<Object> iterator = parameters.iterator();

                message.append("[");
                while (iterator.hasNext()) {
                    message.append(iterator.next());

                    if (iterator.hasNext()) {
                        message.append(" | ");
                    }
                }
                message.append("]");
            }
            LOG.info(message.toString());
        }
    }
}
