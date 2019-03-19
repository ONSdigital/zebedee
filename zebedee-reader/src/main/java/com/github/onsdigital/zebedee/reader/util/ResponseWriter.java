package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.reader.Resource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface ResponseWriter {

    void sendResponse(Object content, HttpServletResponse response) throws IOException;

    void sendResponse(Resource resource, HttpServletResponse response, String encoding) throws IOException;

    void sendNotFound(NotFoundException exception, HttpServletRequest request, HttpServletResponse response) throws IOException;

    void sendBadRequest(BadRequestException exception, HttpServletRequest request, HttpServletResponse response) throws IOException;
}
