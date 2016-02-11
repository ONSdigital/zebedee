package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter;
import com.github.onsdigital.zebedee.model.csdb.CsdbImporter;
import com.github.onsdigital.zebedee.model.csdb.DylanClient;
import com.github.onsdigital.zebedee.model.csdb.HttpDylanClient;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.util.Log;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.util.List;

@Api
public class CsdbNotify {

    // Hold only one instance of DylanClient.
    private static final DylanClient dylanClient = new HttpDylanClient(Configuration.getDylanUrl());

    /**
     * Notify zebedee that a new CSDB file is available
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws ZebedeeException
     */
    @POST
    public void csdbNotify(HttpServletRequest request, HttpServletResponse response, String csdbId) throws IOException, ZebedeeException {
        PrivateKey privateKey = Root.zebedee.applicationKeys.getPrivateKeyFromCache(CsdbImporter.APPLICATION_KEY_ID);

        CsdbImporter.processNotification(
                privateKey,
                csdbId,
                dylanClient,
                Root.zebedee.collections,
                Root.zebedee.keyringCache.schedulerCache);



    }
}
