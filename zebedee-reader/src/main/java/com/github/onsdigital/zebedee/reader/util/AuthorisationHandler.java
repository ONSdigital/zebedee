package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by bren on 31/07/15.
 */
public interface AuthorisationHandler {
    void authorise(HttpServletRequest request, String collectionId) throws IOException, UnauthorizedException, NotFoundException, BadRequestException;
}
