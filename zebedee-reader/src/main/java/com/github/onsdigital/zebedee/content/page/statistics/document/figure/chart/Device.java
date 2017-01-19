package com.github.onsdigital.zebedee.content.page.statistics.document.figure.chart;

/**
 * Created by mcoulthurst on 19/01/2017.
 */
public class Device {

    private String aspectRatio;
    private String labelInterval;
    private boolean isHidden;


    public String getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public String getLabelInterval() {
        return labelInterval;
    }

    public void setLabelInterval(String labelInterval) {
        this.labelInterval = labelInterval;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

}
