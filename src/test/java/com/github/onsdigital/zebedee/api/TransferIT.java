package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by kanemorgan on 25/03/2015.
 */
public class TransferIT {

    private String authenticationToken = "";

    @Before
    public void setUp() throws Exception {
        authenticationToken = LoginIT.login();
    }

    @Test
    public void shouldMoveURIFromSourceToDestination(){

        CollectionDescription source = CollectionIT.createCollection(authenticationToken);
        CollectionDescription destination = CollectionIT.createCollection(authenticationToken);

    }

}
