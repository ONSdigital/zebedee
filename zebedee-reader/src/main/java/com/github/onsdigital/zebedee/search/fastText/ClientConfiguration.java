package com.github.onsdigital.zebedee.search.fastText;

/**
 * Wrapping client connection manager setters to simplify configuration for single host
 */
public class ClientConfiguration {

    private int maxTotalConnection;
    private boolean disableRedirectHandling;

    public ClientConfiguration(int maxTotalConnection, boolean disableRedirectHandling) {
        this.maxTotalConnection = maxTotalConnection;
        this.disableRedirectHandling = disableRedirectHandling;
    }

    public int getMaxTotalConnection() {
        return maxTotalConnection;
    }

    public boolean isDisableRedirectHandling() {
        return disableRedirectHandling;
    }
}
