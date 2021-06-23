package com.github.onsdigital.zebedee.session.store;

import javax.servlet.http.HttpServletRequest;


/**
 * This method creates a ThreadLocal for the validated jwt 
 * from a HTTP request and appropriate key 
 *  as part of implementing dp-identity-api
 * 
 */

public interface SessionsThreadLocal {

    void store(HttpServletRequest request, String secretKey) throws Exception;
}
