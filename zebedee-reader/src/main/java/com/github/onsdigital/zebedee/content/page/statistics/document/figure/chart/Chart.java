package com.github.onsdigital.zebedee.content.page.statistics.document.figure.chart;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;

import java.util.List;
import java.util.Map;

public class Chart extends Page {

    private String title;
    private String subtitle;
    private String filename;
    private String source;
    private String notes;
    private String altText;

    private String unit;
    private String aspectRatio;
    private String chartType;
    private List<Map<String, String>> data;
    private List<String> headers;
    private List<String> series;
    private List<String> categories;
    private Map<String, String> chartTypes;
    private List<List<String>> groups;

    @Override
    public PageType getType() {
        return PageType.chart;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public String getChartType() {
        return chartType;
    }

    public void setChartType(String chartType) {
        this.chartType = chartType;
    }

    public List<Map<String, String>> getData() {
        return data;
    }

    public void setData(List<Map<String, String>> data) {
        this.data = data;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<String> getSeries() {
        return series;
    }

    public void setSeries(List<String> series) {
        this.series = series;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public Map<String, String> getChartTypes() {
        return chartTypes;
    }

    public void setChartTypes(Map<String, String> chartTypes) {
        this.chartTypes = chartTypes;
    }

    public List<List<String>> getGroups() {
        return groups;
    }

    public void setGroups(List<List<String>> groups) {
        this.groups = groups;
    }
}
