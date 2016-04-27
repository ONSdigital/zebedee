package com.github.onsdigital.zebedee.content.page.statistics.document.figure.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by dave on 4/21/16.
 */
public class TableModifications {

    private List<Integer> rowsExcluded = new ArrayList<>();
    private List<Integer> headerRows = new ArrayList<>();
    private List<Integer> headerColumns = new ArrayList<>();

    public List<Integer> getRowsExcluded() {
        return rowsExcluded;
    }

    public TableModifications setRowsExcluded(List<Integer> rowsExcluded) {
        this.rowsExcluded = sort(rowsExcluded);
        return this;
    }

    public List<Integer> getHeaderRows() {
        return headerRows;
    }

    public TableModifications setHeaderRows(List<Integer> headerRows) {
        this.headerRows = sort(headerRows);
        return this;
    }

    public List<Integer> getHeaderColumns() {
        return headerColumns;
    }

    public TableModifications setHeaderColumns(List<Integer> headerColumns) {
        this.headerColumns = sort(headerColumns);
        return this;
    }

    private List<Integer> sort(List<Integer> target) {
        if (target != null & !target.isEmpty()) {
            Collections.sort(target);
            Collections.reverse(target);
        }
        return target;
    }

    public boolean modificationsExist() {
        return this.rowsExcluded.size() > 0 || this.headerRows.size() > 0 || this.headerColumns.size() > 0;
    }
}
