package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.reader.Resource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Wrap calls to static methods in an interface to make it easier to test.
 */
public class ReaderResponseWriter implements ResponseWriter {
    @Override
    public void sendResponse(Object content, HttpServletResponse response) throws IOException {
        ReaderResponseResponseUtils.sendResponse(content, response);
    }

    @Override
    public void sendResponse(Resource resource, HttpServletResponse response, String encoding) throws IOException {
        ReaderResponseResponseUtils.sendResponse(resource, response, encoding);
    }

    @Override
    public void sendNotFound(NotFoundException exception, HttpServletRequest request, HttpServletResponse response) throws IOException {
        ReaderResponseResponseUtils.sendNotFound(exception, request, response);
    }

    @Override
    public void sendBadRequest(BadRequestException exception, HttpServletRequest request, HttpServletResponse response) throws IOException {
        ReaderResponseResponseUtils.sendBadRequest(exception, request, response);
    }
}
