package com.github.onsdigital.zebedee.api.cmd;

import com.github.onsdigital.zebedee.permissions.cmd.CRUD;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsException;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsService;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsServiceImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;

public class DatasetPermissionsRequestHandler implements PermissionsRequestHandler {

    private static final String BREARER_PREFIX = "Bearer ";

    private PermissionsService permissionsService;

    public DatasetPermissionsRequestHandler() {
        this.permissionsService = PermissionsServiceImpl.getInstance();
    }

    public DatasetPermissionsRequestHandler(PermissionsService permissionsService) {
        this.permissionsService = permissionsService;
    }

    @Override
    public CRUD get(HttpServletRequest req, HttpServletResponse resp) throws PermissionsException {
        String sessionID = req.getHeader(FLORENCE_AUTH_HEATHER);
        String serviceToken = req.getHeader(SERVICE_AUTH_HEADER);
        String datasetID = req.getParameter(DATASET_ID_PARAM);
        String collectionID = req.getParameter(COLLECTION_ID_PARAM);

        if (isEmpty(sessionID) && isEmpty(serviceToken)) {
            info().log("invalid permissions request expected user or service auth token but none found");
            throw new PermissionsException("invalid request", SC_BAD_REQUEST);
        }

        if (isNotEmpty(sessionID)) {
            info().log("handling get permissions request for user");
            return permissionsService.getUserDatasetPermissions(sessionID, datasetID, collectionID);
        }

        info().log("handling get permissions request for service account");
        return permissionsService.getServiceDatasetPermissions(parseServiceToken(serviceToken), datasetID);
    }

    String parseServiceToken(String serviceToken) {
        if (serviceToken.startsWith(BREARER_PREFIX)) {
            serviceToken = serviceToken.replaceFirst(BREARER_PREFIX, "");
        }
        return serviceToken;
    }
}
