package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.collection.audit.CollectionHistoryEvent;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;

/**
 * API returning the collection event history for the specified collection. API collectionID as the last section of the
 * URI.
 */
@Api
public class CollectionHistory {

    private static CollectionHistoryDao collectionHistoryDaoImpl = CollectionHistoryDao.getInstance();

    /**
     * Get the collection event history for the specified collection.
     *
     * @param request
     * @param response
     * @return
     * @throws ZebedeeException
     */
    @GET
    public com.github.onsdigital.zebedee.model.collection.audit.CollectionHistory getCollectionEventHistory(
            HttpServletRequest request, HttpServletResponse response)
            throws ZebedeeException {
        String collectionName = URIUtils.getPathSegment(request.getRequestURI(), 2);

        if (StringUtils.isEmpty(collectionName)) {
            throw new BadRequestException("collectionId was not specified.");
        }
        return new com.github.onsdigital.zebedee.model.collection.audit.CollectionHistory(
                collectionHistoryDaoImpl.getCollectionEventHistory(collectionName));
    }
}
