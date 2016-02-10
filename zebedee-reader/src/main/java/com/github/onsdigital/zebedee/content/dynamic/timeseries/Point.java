package com.github.onsdigital.zebedee.content.dynamic.timeseries;

/**
 * Created by bren on 14/08/15.
 * <p>
 * Date, Value point of a time series value
 */
public class Point {

    private String name;
    private Double y;
    private String stringY;

    public Point(String name, String y) {
        this.name = name;
        stringY = y;
        try {
            this.y = Double.parseDouble(y);
        } catch (NumberFormatException e) {
            System.err.println("Timeseries value is not a number");
        }
    }


    public String getName() {
        return name;
    }

    public Double getY() {
        return y;
    }

    public String getStringY() { return stringY; }
}
