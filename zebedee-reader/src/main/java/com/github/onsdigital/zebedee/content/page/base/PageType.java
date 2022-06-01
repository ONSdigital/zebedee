package com.github.onsdigital.zebedee.content.page.base;

import com.google.gson.annotations.SerializedName;

/**
 * Enumerates the different types of pages on the website.
 *
 * @author david
 * @author bren
 */
public enum PageType {

    @SerializedName("home_page")
    HOME_PAGE("Home page"),

    @SerializedName("home_page_census")
    HOME_PAGE_CENSUS("Census home page"),

    @SerializedName("taxonomy_landing_page")
    TAXONOMY_LANDING_PAGE("Taxonomy landing page"),

    @SerializedName("product_page")
    PRODUCT_PAGE("Product page"),

    @SerializedName("bulletin")
    BULLETIN("Bulletin"),

    @SerializedName("article")
    ARTICLE("Article"),

    @SerializedName("article_download")
    ARTICLE_DOWNLOAD("Article download"),

    @SerializedName("timeseries_landing_page")
    TIMESERIES_LANDING_PAGE("Timeseries landing page"),

    @SerializedName("timeseries")
    TIMESERIES("Timeseries page"),

    @SerializedName("data_slice")
    DATA_SLICE("Data slice"),

    @SerializedName("compendium_landing_page")
    COMPENDIUM_LANDING_PAGE("Compendium landing page"),

    @SerializedName("compendium_chapter")
    COMPENDIUM_CHAPTER("Compendium chapter page"),//Resolve parent

    @SerializedName("compendium_data")
    COMPENDIUM_DATA("Compendium data page"),

    @SerializedName("static_landing_page")
    STATIC_LANDING_PAGE("Static landing page"),

    @SerializedName("static_article")
    STATIC_ARTICLE("Static article"), //With table of contents

    @SerializedName("static_methodology")
    STATIC_METHODOLOGY("Static methodology page"),

    @SerializedName("static_methodology_download")
    STATIC_METHODOLOGY_DOWNLOAD("Static methodology download page"),

    @SerializedName("static_page")
    STATIC_PAGE("Static page"), //Pure markdown

    @SerializedName("static_qmi")
    STATIC_QMI("Static QMI page"),

    @SerializedName("static_foi")
    STATIC_FOI("Static FOI page"),

    @SerializedName("static_adhoc")
    STATIC_ADHOC("Static adhoc page"),

    @SerializedName("interactive")
    INTERACTIVE("Interactive page"),

    @SerializedName("dataset")
    DATASET("Dataset page"),

    @SerializedName("dataset_landing_page")
    DATASET_LANDING_PAGE("Dataset landing page"),

    @SerializedName("api_dataset_landing_page")
    API_DATASET_LANDING_PAGE("API Dataset landing page"),

    @SerializedName("api_dataset")
    API_DATASET("API Dataset"),

    @SerializedName("timeseries_dataset")
    TIMESERIES_DATASET("Timeseries dataset page"),

    @SerializedName("release")
    RELEASE("Release page"),

    @SerializedName("reference_tables")
    REFERENCE_TABLES("Reference tables"),

    @SerializedName("chart")
    CHART("Chart page"),

    @SerializedName("table")
    TABLE("Table page"),

    @SerializedName("image")
    IMAGE("Image page"),

    @SerializedName("visualisation")
    VISUALISATION("Visualisation page"),

    @SerializedName("equation")
    EQUATION("Equation page");

    private final String displayName;

    PageType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLabel() {
        try {
            return PageType.class.getField(this.name()).getAnnotation(SerializedName.class).value();
        } catch (NoSuchFieldException | SecurityException e) {
            throw new RuntimeException("error getting page type label", e);
        }
    }
}
