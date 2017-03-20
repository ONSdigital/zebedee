package com.github.onsdigital.zebedee.content.page.statistics.document.figure.chart;

import java.util.Map;
/**
 * Created by carlhembrough on 10/01/2017.
 */
public class Annotation {

    private String id;
    private String x;
    private String y;
    private String title;
    private String orientation;
    private boolean isHidden;
    private boolean isPlotline;
    private boolean isPlotband;
    private String bandWidth;
    private String width;
    private String height;

    private Map<String, AnnotationDevice> devices;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public boolean isPlotline() {
        return isPlotline;
    }

    public void setPlotline(boolean plotline) {
        isPlotline = plotline;
    }

    public boolean isPlotband() {
        return isPlotband;
    }

    public void setPlotband(boolean plotband) {
        isPlotband = plotband;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getBandWidth() {
        return bandWidth;
    }

    public void setBandWidth(String bandWidth) {
        this.bandWidth = bandWidth;
    }
    
    public Map<String, AnnotationDevice> getDevices() {
        return devices;
    }

    public void setDevices(Map<String, AnnotationDevice> devices) {
        this.devices = devices;
    }
}
