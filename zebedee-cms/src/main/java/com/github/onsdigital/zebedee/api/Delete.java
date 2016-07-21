package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import java.util.Optional;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

/**
 * Mark content to be deleted.
 */
@Api
public class Delete {

    @DELETE
    public boolean markContentToBeDeleted(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> uri = RequestUtils.getURIParameter(request);
        if (!uri.isPresent()) {
            return false;
        }
        logDebug("Marking content for deletion").path(uri.get()).log();
        return true;
    }
}
