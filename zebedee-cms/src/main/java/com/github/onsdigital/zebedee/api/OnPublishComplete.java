package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.cryptolite.Password;
import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.util.ContentTree;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Endpoint to be called when a publish takes place.
 * Can be used to clear cached items / search indexes etc.
 */
@Api
public class OnPublishComplete {

    /**
     * Generates new reindexing key/hash values.
     *
     * @param args Not used.
     */
    public static void main(String[] args) {
        String key = Random.password(64);
        logDebug("Key added to environment").addParameter("key", key).log();
        logDebug("Key hash (for REINDEX_KEY_HASH)").addParameter("keyHash", Password.hash(key)).log();
    }

    @POST
    public Object onPublishComplete(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {

        logDebug("Clearing browser tree cache").log();
        ContentTree.dropCache();
        response.setStatus(HttpStatus.OK_200);
        return "OnPublishComplete handler finished";
    }
}
