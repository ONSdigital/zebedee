package com.github.onsdigital.zebedee.content.page.base;

/**
 * Enumerates the different types of pages on the website.
 * <p>
 * Strictly these would be uppercase, but "shouty caps" looks wrong when
 * serialised to Json. There are ways around it, but the simplest solution is to
 * use lowercase - it's not worth the complexity.
 *
 * @author david
 * @author bren
 */
public enum PageType {

    home_page("Home page"),
    home_page_census("Census home page"),
    taxonomy_landing_page("Taxonomy landing page"),
    product_page("Product page"),
    bulletin("Bulletin"),
    article("Article"),
    article_download("Article download"),
    timeseries_landing_page("Timeseries landing page"),
    timeseries("Timeseries page"),
    data_slice("Data slice"),
    compendium_landing_page("Compendium landing page"),
    compendium_chapter("Compendium chapter page"),//Resolve parent
    compendium_data("Compendium data page"),
    static_landing_page("Static landing page"),
    static_article("Static article"), //With table of contents
    static_methodology("Static methodology page"),
    static_methodology_download("Static methodology download page"),
    static_page("Static page"), //Pure markdown
    static_qmi("Static QMI page"),
    static_foi("Static FOI page"),
    static_adhoc("Static adhoc page"),
    dataset("Dataset page"),
    dataset_landing_page("Dataset landing page"),
    timeseries_dataset("Timeseries dataset page"),
    release("Release page"),
    reference_tables("Reference tables"),
    chart("Chart page"),
    table("Table page"),
    image("Image page"),
    visualisation("Visualisation page"),
    equation("Equation page");

    private final String displayName;

    PageType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
