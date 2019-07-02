package com.github.onsdigital.zebedee.api.cmd;

import com.github.onsdigital.zebedee.permissions.cmd.CRUD;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Definition of a CMD permissions request handler. Return {@link CRUD} permissions granted to the caller for the
 * action they attempting to perform.
 */
@FunctionalInterface
public interface PermissionsRequestHandler {

    /**
     * Dataset ID request parameter key
     */
    String DATASET_ID_PARAM = "dataset_id";

    /**
     * Collection ID request parameter key
     */
    String COLLECTION_ID_PARAM = "collection_id";

    /**
     * Service auth token request header.
     */
    String SERVICE_AUTH_HEADER = "Authorization";

    /**
     * User auth token request header.
     */
    String FLORENCE_AUTH_HEATHER = "X-Florence-Token";

    /**
     * Return {@link CRUD} permissions granted to the caller for the action they attempting to perform.
     *
     * @param req  {@link HttpServletRequest}.
     * @param resp {@link HttpServletResponse}
     * @return {@link CRUD} of permissions granted to the caller for this aciton.
     * @throws PermissionsException problem getting the caller permissions.
     */
    CRUD get(HttpServletRequest req, HttpServletResponse resp) throws PermissionsException;
}
