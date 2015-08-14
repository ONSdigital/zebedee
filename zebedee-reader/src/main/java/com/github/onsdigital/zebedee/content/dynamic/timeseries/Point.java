package com.github.onsdigital.zebedee.content.dynamic.timeseries;

/**
 * Created by bren on 14/08/15.
 * <p>
 * Date, Value point of a time series value
 */
public class Point {

    private String date;
    private String value;


    public Point(String date, String value) {
        this.date = date;
        this.value = value;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
