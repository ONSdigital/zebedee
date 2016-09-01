package com.github.onsdigital.zebedee.data.processing;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thomasridd on 1/23/16.
 */
public class DataGridRow {
    String id;
    String label;
    List<String> cells = new ArrayList<>();

    public DataGridRow(String id) {
        this(id, id);
    }
    public DataGridRow(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public DataGridRow(String id, int capacity) {
        this.id = id;
        this.label = id;
        for (int i = 0; i < capacity; i++)
            cells.add("");
    }

    public void add(String value) {
        if (value == null) {
            cells.add("");
        } else {
            cells.add(value);
        }
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getCells() {
        return cells;
    }
}
