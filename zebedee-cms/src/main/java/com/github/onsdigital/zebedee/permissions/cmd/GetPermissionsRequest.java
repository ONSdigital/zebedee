package com.github.onsdigital.zebedee.permissions.cmd;

import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.servlet.http.HttpServletRequest;

/**
 * @deprecated in favour of the dp-permissions-api. Once all dataset related APIs have been updated to use the
 *             dp-authorisation v2 library and JWT sessions are in use, this service will be removed.
 */
@Deprecated
public class GetPermissionsRequest {

    private static final String DATASET_ID_PARAM = "dataset_id";
    private static final String COLLECTION_ID_PARAM = "collection_id";
    private static final String SERVICE_AUTH_HEADER = "Authorization";

    private String collectionID;
    private String datasetID;
    private String sessionID;
    private String serviceToken;

    public GetPermissionsRequest(HttpServletRequest req) {
        this.sessionID = RequestUtils.getSessionId(req);

        // TODO: Remove after new service user JWT auth is implemented and all automated users are using JWT sessions
        String authHeader = req.getHeader(SERVICE_AUTH_HEADER);
        if (authHeader != null && !authHeader.contains(".")) {
            this.serviceToken = RequestUtils.removeBearerPrefixIfPresent(authHeader);
        }

        this.datasetID = req.getParameter(DATASET_ID_PARAM);
        this.collectionID = req.getParameter(COLLECTION_ID_PARAM);
    }

    public GetPermissionsRequest(String sessionID, String serviceToken, String datasetID, String collectionID) {
        this.sessionID = sessionID;
        this.serviceToken = RequestUtils.removeBearerPrefixIfPresent(serviceToken);
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
