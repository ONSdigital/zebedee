package com.github.onsdigital.zebedee.content.dynamic.timeseries;

import static com.github.onsdigital.zebedee.logging.ReaderLogger.error;

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
            error().logException(e, "timeseries point value could not be parsed to double");
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
