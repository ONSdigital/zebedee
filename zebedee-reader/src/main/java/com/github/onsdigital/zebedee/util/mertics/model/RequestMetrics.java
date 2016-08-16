package com.github.onsdigital.zebedee.util.mertics.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequestMetrics extends SplunkEvent {

    private static final Path HOME_URI = Paths.get("/");

    protected transient long interceptTime;
    protected long timeTaken;
    protected String api;
    protected String httpMethod;
    protected String requestedURI;

    public RequestMetrics(HttpServletRequest request) {
        this.interceptTime = System.currentTimeMillis();
        this.httpMethod = request.getMethod();
        this.requestedURI = request.getParameter("uri");

        Path uri = Paths.get(request.getRequestURI());

        if (HOME_URI.equals(uri)) {
            this.api = uri.toString();
        } else {
            List<Path> uriPaths = new ArrayList<>();
            while (uri.getParent() != null) {
                uriPaths.add(uri);
                uri = uri.getParent();
            }
            Collections.reverse(uriPaths);
            this.api = uriPaths.get(0).toString();
        }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        RequestMetrics that = (RequestMetrics) o;

        return new EqualsBuilder()
                .append(api, that.api)
                .append(httpMethod, that.httpMethod)
                .append(requestedURI, that.requestedURI)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(api)
                .append(httpMethod)
                .append(requestedURI)
                .toHashCode();
    }
}
