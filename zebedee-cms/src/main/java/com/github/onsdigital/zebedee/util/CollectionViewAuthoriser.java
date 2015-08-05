package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.reader.util.AuthorisationHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;

/**
 * Created by bren on 31/07/15.
 */
public class CollectionViewAuthoriser implements AuthorisationHandler {
    @Override
    public void authorise(HttpServletRequest request, String collectionId) throws IOException, UnauthorizedException, NotFoundException {
        Session session = Root.zebedee.sessions.get(request);

        CollectionDescription collectionDescription = new CollectionDescription();
        collectionDescription.id = collectionId;


        // Authorisation
        if (session == null
                || !Root.zebedee.permissions.canView(session.email,
                collectionDescription)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }
    }
}