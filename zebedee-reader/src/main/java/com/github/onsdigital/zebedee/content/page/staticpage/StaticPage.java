package com.github.onsdigital.zebedee.content.page.staticpage;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.staticpage.base.BaseStaticPage;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.FigureSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bren on 30/06/15.
 *
 * Simple static page with only markdown content
 */
public class StaticPage extends BaseStaticPage {

    private List<FigureSection> charts = new ArrayList<>();
    private List<FigureSection> tables = new ArrayList<>();
    private List<FigureSection> images = new ArrayList<>();
    private List<FigureSection> equations = new ArrayList<>();

    public List<FigureSection> getCharts() {
        return charts;
    }

    public void setCharts(List<FigureSection> charts) {
        this.charts = charts;
    }

    public List<FigureSection> getTables() {
        return tables;
    }

    public void setTables(List<FigureSection> tables) {
        this.tables = tables;
    }

    public List<FigureSection> getImages() {
        return images;
    }

    public void setImages(List<FigureSection> images) {
        this.images = images;
    }

    public List<FigureSection> getEquations() {
        return equations;
    }

    public void setEquations(List<FigureSection> equations) {
        this.equations = equations;
    }

    @Override
    public PageType getType() {
        return PageType.static_page;
    }
}
