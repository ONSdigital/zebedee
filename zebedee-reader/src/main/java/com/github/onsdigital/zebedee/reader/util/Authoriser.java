package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by bren on 31/07/15.
 */
public interface Authoriser {
    void authorise(HttpServletRequest request) throws IOException, UnauthorizedException;
}
