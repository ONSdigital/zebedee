package com.github.onsdigital.zebedee.session.store;

import com.github.onsdigital.impl.UserDataPayload;

import javax.servlet.http.HttpServletRequest;

public interface SessionsThreadLocal {

    void store(HttpServletRequest request, String secretKey) throws Exception;
}
