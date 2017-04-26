package com.github.onsdigital.zebedee.content.page.statistics.document.figure.chart;

/**
 * Store the chart coordinates for each annotation for different devices/breakpoints
 */
public class AnnotationDevice {

    private String id;
    private String x;
    private String y;

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

}
