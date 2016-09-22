package com.github.onsdigital.zebedee.util.mertics.events;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.time.FastDateFormat;
import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Objects.requireNonNull;

public class SplunkEvent {

    public static final String API_KEY = "api";
    public static final String REQUESTED_URI_KEY = "requestedURI";
    public static final String HTTP_METHOD_KEY = "httpMethod";
    public static final String INTERCEPT_TIME_KEY = "interceptTime";
    public static final String TIME_TAKEN_KEY = "timeTaken";
    public static final String PING_TIME_KEY = "pingTime";
    public static final String METRICS_TYPE_KEY = "metricsType";
    public static final String COLLECTION_PUBLISH_TIME = "collectionsPublishTime";
    public static final String COLLECTION_PUBLISH_FILE_COUNT = "collectionsPublishFileCount";
    public static final String COLLECTION_PUBLISH_TYPE = "collectionsPublishType";
    public static final String COLLECTION_PUBLISH_START_TIME = "collectionsPublishStartTime";
    public static final String COLLECTION_ID = "collectionId";
    public static final String EVENT_KEY = "event";
    public static final String URI_KEY = "uri";
    private static final Path HOME_URI = Paths.get("/");

    private static FastDateFormat publishTimeFormat = FastDateFormat.getInstance("HH:mm", TimeZone.getTimeZone("Europe/London"));

    private Map<String, Object> event;

    public SplunkEvent(Map<String, Object> fields) {
        this.event = new ImmutableMap.Builder<String, Object>().put(EVENT_KEY, fields).build();
    }

    public String toJson() throws IOException {
        return new ObjectMapper().writeValueAsString(event);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        SplunkEvent that = (SplunkEvent) o;

        return new EqualsBuilder()
                .append(event, that.event)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(event)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(EVENT_KEY, event)
                .toString();
    }

    public static class Builder {

        private Map<String, Object> fields;
        private HttpServletRequest request;

        public Builder() {
            this.fields = new HashMap<>();
        }

        public Builder api(String api) {
            fields.put(API_KEY, api);
            return this;
        }

        public Builder requestedURI(String requestedURI) {
            fields.put(REQUESTED_URI_KEY, requestedURI);
            return this;
        }

        public Builder httpMethod(String httpMethod) {
            fields.put(HTTP_METHOD_KEY, httpMethod);
            return this;
        }

        public Builder interceptTime(long interceptTime) {
            fields.put(INTERCEPT_TIME_KEY, interceptTime);
            return this;
        }

        public Builder timeTaken(long timeTaken) {
            fields.put(TIME_TAKEN_KEY, timeTaken);
            return this;
        }

        public Builder pingTime(long pingTime) {
            fields.put(PING_TIME_KEY, pingTime);
            return this;
        }

        public Builder collectionId(String collectionId) {
            fields.put(COLLECTION_ID, collectionId);
            return this;
        }

        public Builder collectionPublishTimeTaken(long collectionPublishTime) {
            fields.put(COLLECTION_PUBLISH_TIME, collectionPublishTime);
            return this;
        }

        public Builder collectionPublishFileCount(int collectionPublishFileCount) {
            fields.put(COLLECTION_PUBLISH_FILE_COUNT, collectionPublishFileCount);
            return this;
        }

        public Builder request(HttpServletRequest request) {
            this.request = request;
            return this;
        }

        public Builder collectionPublishType(String collectionType) {
            fields.put(COLLECTION_PUBLISH_TYPE, collectionType);
            return this;
        }

        public Builder collectionPublishTime(Date publishDate) {
            fields.put(COLLECTION_PUBLISH_START_TIME, publishTimeFormat.format(publishDate));
            return this;
        }

        public Builder addField(String fieldName, Object value) {
            this.fields.put(fieldName, value);
            return this;
        }

        public Object get(String fieldName) {
            return this.fields.get(fieldName);
        }

        /**
         * Build a SplunkEvent.
         *
         * @param metricsType the {@link MetricsType} sent to splunk to determined the type of metrics this event
         *                    belongs to.
         * @param excludeKeys a list of fields to ignore when creating the {@link SplunkEvent} - {@link MetricsType}
         *                    cannot be excluded.
         * @return a SplunkEvent.
         */
        public SplunkEvent build(MetricsType metricsType, String... excludeKeys) {
            requireNonNull(metricsType, "MetricsType is mandatory and cannot be null");
            extractRequestDetails();
            fields.put(METRICS_TYPE_KEY, metricsType);

            for (String excludeKey : excludeKeys) {
                if (!StringUtils.equals(METRICS_TYPE_KEY, excludeKey)) {
                    this.fields.remove(excludeKey);
                }
            }
            return new SplunkEvent(this.fields);
        }

        private void extractRequestDetails() {
            if (this.request != null) {
                this.httpMethod(request.getMethod())
                        .requestedURI(request.getParameter(URI_KEY))
                        .api(getApi(request).toString());
            }
        }

        private Path getApi(HttpServletRequest request) {
            if (request == null || HOME_URI.equals(request.getRequestURI())) {
                return HOME_URI;
            }
            Path apiUri = Paths.get(request.getRequestURI());
            List<Path> uriPaths = new ArrayList<>();
            while (apiUri.getParent() != null) {
                uriPaths.add(apiUri);
                apiUri = apiUri.getParent();
            }
            Collections.reverse(uriPaths);
            return uriPaths.get(0);
        }
    }
}
