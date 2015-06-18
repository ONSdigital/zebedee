package com.github.onsdigital.zebedee.data.json;

import java.util.HashMap;

/**
 * Created by thomasridd on 05/06/15.
 */
public class TimeSeriesObject {
    public String taxi;
    public String name;
    public boolean hasYearly = false;
    public boolean hasMonthly = false;
    public boolean hasQuarterly = false;

    public transient boolean shouldHaveYearly = false;
    public transient boolean shouldHaveMonthly = false;
    public transient boolean shouldHaveQuarterly = false;

    public HashMap<String, TimeSeriesPoint> points = new HashMap<>();
}
