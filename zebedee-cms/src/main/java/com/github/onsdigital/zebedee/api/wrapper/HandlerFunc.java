package com.github.onsdigital.zebedee.api.wrapper;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface HandlerFunc<T, R> {

    R doHandle(HttpServletRequest req, HttpServletResponse resp, T t) throws IOException, ZebedeeException;
}
