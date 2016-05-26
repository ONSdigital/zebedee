package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.collection.audit.CollectionAuditHistory;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;

import static com.github.onsdigital.zebedee.model.collection.audit.AuditAction.COLLECTION_CREATED;

/**
 * API returning the collection audit history for the specified collection.
 */
@Api
public class CollectionAudit {

    // TODO remove stub once fully implemented.
    private static final CollectionAuditHistory SAMPLE_HISTORY;

    static {
        SAMPLE_HISTORY = new CollectionAuditHistory();
        SAMPLE_HISTORY.add(new com.github.onsdigital.zebedee.model.collection.audit.CollectionAudit.Builder()
                .collectionId("1234567890")
                .user("Batman@JusticeLeague.com")
                .eventAction(COLLECTION_CREATED).build());
        SAMPLE_HISTORY.add(new com.github.onsdigital.zebedee.model.collection.audit.CollectionAudit.Builder()
                .collectionId("9876543210")
                .user("Superman@JusticeLeague.com")
                .eventAction(COLLECTION_CREATED).build());
        SAMPLE_HISTORY.add(new com.github.onsdigital.zebedee.model.collection.audit.CollectionAudit.Builder()
                .collectionId("66666666")
                .user("WonderWoman@JusticeLeague.com")
                .eventAction(COLLECTION_CREATED).build());
    }

    /**
     * Get the collection audit history for the specified collection.
     *
     * @param request
     * @param response
     * @return
     * @throws ZebedeeException
     */
    @GET
    public CollectionAuditHistory getAuditForCollection(HttpServletRequest request, HttpServletResponse response) throws ZebedeeException {
        String collectionId = RequestUtils.getCollectionId(request);

        if (StringUtils.isEmpty(collectionId)) {
            throw new BadRequestException("CollectionID was not specified.");
        }

        return SAMPLE_HISTORY;
    }
}
