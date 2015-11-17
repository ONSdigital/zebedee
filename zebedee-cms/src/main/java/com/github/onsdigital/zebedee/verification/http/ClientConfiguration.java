package com.github.onsdigital.zebedee.verification.http;

/**
 * Wrapping client connection manager setters to simplify configuration for single host
 */
public class ClientConfiguration {

    private Integer maxTotalConnection;
    private boolean disableRedirectHandling;

    public ClientConfiguration() {
    }

    public void setMaxTotalConnection(int maxConnection) {
        this.maxTotalConnection = maxConnection;
    }

    public Integer getMaxTotalConnection() {
        return maxTotalConnection;
    }

    public void setDisableRedirectHandling(boolean disableRedirectHandling) {
        this.disableRedirectHandling = disableRedirectHandling;
    }

    public boolean isDisableRedirectHandling() {
        return disableRedirectHandling;
    }
}
