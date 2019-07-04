package com.github.onsdigital.zebedee.permissions.cmd;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.servlet.http.HttpServletRequest;

public class GetPermissionsRequest {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DATASET_ID_PARAM = "dataset_id";
    private static final String COLLECTION_ID_PARAM = "collection_id";
    private static final String SERVICE_AUTH_HEADER = "Authorization";
    private static final String FLORENCE_AUTH_HEATHER = "X-Florence-Token";

    private String collectionID;
    private String datasetID;
    private String sessionID;
    private String serviceToken;

    public GetPermissionsRequest(HttpServletRequest req) {
        this.sessionID = req.getHeader(FLORENCE_AUTH_HEATHER);

        String serviceTokenHeader = req.getHeader(SERVICE_AUTH_HEADER);
        this.serviceToken = removeBearerPrefixIfPresent(serviceTokenHeader);

        this.datasetID = req.getParameter(DATASET_ID_PARAM);
        this.collectionID = req.getParameter(COLLECTION_ID_PARAM);
    }

    public GetPermissionsRequest(String sessionID, String serviceToken, String datasetID, String collectionID) {
        this.sessionID = sessionID;
        this.serviceToken = removeBearerPrefixIfPresent(serviceToken);
        this.datasetID = datasetID;
        this.collectionID = collectionID;
    }

    public String getCollectionID() {
        return collectionID;
    }

    public String getDatasetID() {
        return datasetID;
    }

    public String getSessionID() {
        return sessionID;
    }

    public String getServiceToken() {
        return serviceToken;
    }

    private String removeBearerPrefixIfPresent(String serviceToken) {
        if (StringUtils.isEmpty(serviceToken)) {
            return serviceToken;
        }

        if (serviceToken.startsWith(BEARER_PREFIX)) {
            serviceToken = serviceToken.replaceFirst(BEARER_PREFIX, "");
        }
        return serviceToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GetPermissionsRequest request = (GetPermissionsRequest) o;

        return new EqualsBuilder()
                .append(getCollectionID(), request.getCollectionID())
                .append(getDatasetID(), request.getDatasetID())
                .append(getSessionID(), request.getSessionID())
                .append(getServiceToken(), request.getServiceToken())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getCollectionID())
                .append(getDatasetID())
                .append(getSessionID())
                .append(getServiceToken())
                .toHashCode();
    }
}
