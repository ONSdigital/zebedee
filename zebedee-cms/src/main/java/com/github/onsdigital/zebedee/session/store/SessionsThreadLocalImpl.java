package com.github.onsdigital.zebedee.session.store;

import com.github.onsdigital.exceptions.*;

import com.github.onsdigital.exceptions.SessionsDecodeException;
import com.github.onsdigital.exceptions.SessionsTokenExpiredException;
import com.github.onsdigital.exceptions.SessionsVerificationException;
import com.github.onsdigital.impl.*;
import com.github.onsdigital.interfaces.*;
import javax.servlet.http.HttpServletRequest;

public class SessionsThreadLocalImpl implements SessionsThreadLocal {

    private static HttpServletRequest request;
    private static String secretKey;
    private static ThreadLocal<UserDataPayload> store =
            new ThreadLocal<>();

    private JWTHandler jwtHandler;


    public SessionsThreadLocalImpl(HttpServletRequest request, String secretKey) {
//  public SessionsThreadLocalImpl(HttpServletRequest request , JWTHandler jwtHandler, String secretKey) {
        this.request = request;
//        this.jwtHandler = jwtHandler;
        this.secretKey = secretKey;

    }

    @Override
    public void store(HttpServletRequest request, String secretKey ) throws Exception {

        String token = request.getHeader("Authorization");


        UserDataPayload jwtData;
        try {
             jwtData = this.jwtHandler.verifyJWT(token, secretKey);
        } catch (JWTTokenExpiredException e) {
            throw new SessionsTokenExpiredException(e.getMessage(), e);
        } catch (JWTVerificationException e) {
            throw new SessionsVerificationException(e.getMessage(), e);
        } catch (JWTDecodeException e) {
            throw new SessionsDecodeException(e.getMessage(), e);
        }

        store.set(jwtData);

    }

    public static ThreadLocal<UserDataPayload> getStore() {
        return store;
    }
}
