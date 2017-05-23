package com.github.onsdigital.zebedee.content.page.statistics.document.figure.chart;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.FigureBase;

import java.util.List;
import java.util.Map;

public class Chart extends FigureBase {

    private String subtitle;
    private String filename;
    private String source;
    private String notes;
    private String altText;
    private String labelInterval;
    private String decimalPlaces;
    private String decimalPlacesYaxis;
    private String palette;
    private String xAxisPos;
    private String yAxisPos;
    private String yAxisMax;
    private String yMin;
    private String yMax;
    private String yAxisInterval;
    private String highlight;
    private String alpha;

    private String unit;
    private String xAxisLabel;
    private String aspectRatio;
    private String chartType;
    private List<Map<String, String>> data;
    private List<String> headers;
    private List<String> series;
    private List<String> categories;
    private Map<String, String> chartTypes;
    private Map<String, String> lineTypes;
    private List<List<String>> groups;
    private Boolean startFromZero;
    private Boolean finishAtHundred;
    private Boolean isStacked;
    private Boolean isReversed;
    private Boolean showTooltip;
    private Boolean showMarker;
    private Boolean hasLineBreak;
    private Boolean hasConnectNull;
    private Boolean isEditor;

    private List<Annotation> annotations;
    private Map<String, Device> devices;

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }

    public Map<String, Device> getDevices() {
        return devices;
    }

    public void setDevices(Map<String, Device> devices) {
        this.devices = devices;
    }

    @Override
    public PageType getType() {
        return PageType.chart;
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

    public Map<String, String> getLineTypes() {
        return lineTypes;
    }

    public void setLineTypes(Map<String, String> lineTypes) {
        this.lineTypes = lineTypes;
    }

    public List<List<String>> getGroups() {
        return groups;
    }

    public void setGroups(List<List<String>> groups) {
        this.groups = groups;
    }

    public String getLabelInterval() {
        return labelInterval;
    }

    public void setLabelInterval(String labelInterval) {
        this.labelInterval = labelInterval;
    }

    public String getDecimalPlaces() {
        return decimalPlaces;
    }

    public void setDecimalPlaces(String decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public String getDecimalPlacesYaxis() {
        return decimalPlacesYaxis;
    }

    public void setDecimalPlacesYaxis(String decimalPlacesYaxis) {
        this.decimalPlacesYaxis = decimalPlacesYaxis;
    }

    public String getPalette() {
        return palette;
    }

    public void setPalette(String palette) {
        this.palette = palette;
    }

    public String getxAxisPos() {
        return xAxisPos;
    }

    public void setxAxisPos(String xAxisPos) {
        this.xAxisPos = xAxisPos;
    }

    public String getyAxisPos() {
        return yAxisPos;
    }

    public void setyAxisPos(String yAxisPos) {
        this.yAxisPos = yAxisPos;
    }

    public String getyAxisMax() {
        return yAxisMax;
    }

    public void setyAxisMax(String yAxisMax) {
        this.yAxisMax = yAxisMax;
    }

    public String getyMin() {
        return yMin;
    }

    public void setyMin(String yMin) {
        this.yMin = yMin;
    }

    public String getyMax() {
        return yMax;
    }

    public void setyMax(String yMax) {
        this.yMax = yMax;
    }

    public String getyAxisInterval() {
        return yAxisInterval;
    }

    public void setyAxisInterval(String yAxisInterval) {
        this.yAxisInterval = yAxisInterval;
    }

    public String getHighlight() {
        return highlight;
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    public String getAlpha() {
        return alpha;
    }

    public void setAlpha(String alpha) {
        this.alpha = alpha;
    }

    public String getxAxisLabel() {
        return xAxisLabel;
    }

    public void setxAxisLabel(String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;
    }

    public Boolean getStartFromZero() {
        return startFromZero;
    }

    public void setStartFromZero(Boolean startFromZero) {
        this.startFromZero = startFromZero;
    }

    public Boolean getFinishAtHundred() {
        return finishAtHundred;
    }

    public void setFinishAtHundred(Boolean finishAtHundred) {
        this.finishAtHundred = finishAtHundred;
    }

    public Boolean getShowTooltip() {
        return showTooltip;
    }

    public void setShowTooltip(Boolean showTooltip) {
        this.showTooltip = showTooltip;
    }

    public Boolean getShowMarker() {
        return showMarker;
    }

    public void setShowMarker(Boolean showMarker) {
        this.showMarker = showMarker;
    }

    public Boolean getIsStacked() {
        return isStacked;
    }

    public void setIsStacked(Boolean isStacked) {
        this.isStacked = isStacked;
    }

    public Boolean getIsReversed() {
        return isReversed;
    }

    public void setIsReversed(Boolean isReversed) {
        this.isReversed = isReversed;
    }

    public Boolean getHasLineBreak() {
        return hasLineBreak;
    }

    public void setHasLineBreak(Boolean hasLineBreak) {
        this.hasLineBreak = hasLineBreak;
    }

    public Boolean getHasConnectNull() {
        return hasConnectNull;
    }

    public void setHasConnectNull(Boolean hasConnectNull) {
        this.hasConnectNull = hasConnectNull;
    }

    public Boolean getIsEditor() {
        return isEditor;
    }

    public void setIsEditor(Boolean isEditor) {
        this.isEditor = isEditor;
    }
}
