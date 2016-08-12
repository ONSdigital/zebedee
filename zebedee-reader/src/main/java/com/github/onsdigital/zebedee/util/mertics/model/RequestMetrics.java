package com.github.onsdigital.zebedee.util.mertics.model;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RequestMetrics extends SplunkEvent {

    private static final Path HOME_URI = Paths.get("/");

    private transient long interceptTime;
    private long timeTaken;
    private String api;
    private String httpMethod;
    private String requestedURI;

    public RequestMetrics(HttpServletRequest request) {
        this.interceptTime = System.currentTimeMillis();
        this.httpMethod = request.getMethod();
        this.requestedURI = request.getParameter("uri");

        Path uri = Paths.get(request.getRequestURI());
        Path api = uri;

        while (!HOME_URI.equals(uri) && !uri.getParent().equals(HOME_URI)) {
            api = uri.getParent();
        }
        this.api = HOME_URI.resolve(api).toString();
    }

    public long getInterceptTime() {
        return interceptTime;
    }

    public long getTimeTaken() {
        return timeTaken;
    }

    public String getApi() {
        return api;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getRequestedURI() {
        return requestedURI;
    }

    public void stopTimer() {
        this.timeTaken = System.currentTimeMillis() - interceptTime;
    }
}
