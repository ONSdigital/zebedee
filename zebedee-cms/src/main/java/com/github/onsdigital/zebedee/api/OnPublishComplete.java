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
        System.out.println("Key (add to environment): " + key);
        System.out.println("Key hash (for REINDEX_KEY_HASH)" + Password.hash(key));
    }

    @POST
    public Object onPublishComplete(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {

        System.out.println("Clearing browser tree cache");
        ContentTree.dropCache();
        response.setStatus(HttpStatus.OK_200);
        return "OnPublishComplete handler finished";
    }
}
