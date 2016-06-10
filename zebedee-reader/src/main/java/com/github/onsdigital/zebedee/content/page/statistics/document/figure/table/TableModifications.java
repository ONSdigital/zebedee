package com.github.onsdigital.zebedee.content.page.statistics.document.figure.table;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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

    public List<Integer> getHeaderRows() {
        return headerRows;
    }

    public List<Integer> getHeaderColumns() {
        return headerColumns;
    }

    public List<Integer> getRowsExcluded() {
        return rowsExcluded;
    }

    public void setRowsExcluded(List<Integer> rowsExcluded) {
        if (rowsExcluded != null) {
            this.rowsExcluded = rowsExcluded;
        }
    }

    public void setHeaderRows(List<Integer> headerRows) {
        if (headerRows != null) {
            this.headerRows = headerRows;
        }
    }

    public void setHeaderColumns(List<Integer> headerColumns) {
        if (headerColumns != null) {
            this.headerColumns = headerColumns;
        }
    }

    private List<Integer> sortAscending(List<Integer> target) {
        if (target != null & !target.isEmpty()) {
            Collections.sort(target);
            Collections.reverse(target);
        }
        return target;
    }

    public void sorted() {
        this.rowsExcluded = sortAscending(rowsExcluded);
        this.headerRows = sortAscending(headerRows);
        this.headerColumns = sortAscending(headerColumns);
    }

    public boolean modificationsExist() {
        return this.rowsExcluded.size() > 0 || this.headerRows.size() > 0 || this.headerColumns.size() > 0;
    }

    public String summary() {
        return "rowsExcluded: " + rowsExcluded + ", headerRows: " + headerRows + ", headerColumns: " + headerColumns;
    }
}
