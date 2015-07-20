package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.content.page.base.Page;
import com.github.onsdigital.content.page.base.PageType;
import com.github.onsdigital.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.content.page.statistics.document.article.Article;
import com.github.onsdigital.content.page.statistics.document.bulletin.Bulletin;

import com.github.onsdigital.content.page.taxonomy.ProductPage;
import com.github.onsdigital.content.page.taxonomy.TaxonomyLandingPage;
import com.github.onsdigital.content.util.ContentUtil;
import com.github.onsdigital.zebedee.Zebedee;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.util.*;

/**
 * Created by thomasridd on 06/07/15.
 */
public class Librarian {
    private static final Gson gson = new Gson();
    private static boolean catalogueMade = false;

    Zebedee zebedee;
    int checkedUris = 0;

    List<HashMap<String, String>> bulletins = new ArrayList<>();
    List<HashMap<String, String>> articles = new ArrayList<>();
    List<HashMap<String, String>> pages = new ArrayList<>();
    List<HashMap<String, String>> datasets = new ArrayList<>();

    public List<HashMap<String, String>> contentErrors = new ArrayList<>();

    public List<String> invalidJson = new ArrayList<>();

    public Librarian(Zebedee zebedee) {
        this.zebedee = zebedee;
    }

    /**
     * Build a list of all content
     *
     * @throws IOException
     */
    public void catalogue() throws IOException {
        findBulletins();
        findArticles();
        findPages();
        findDatasets();
        catalogueMade  = true;
    }

    private void findBulletins () throws IOException {
        List<Path> bulletins = launchpadMatching(bulletinMatcher());
        for (Path bulletinPath: bulletins) {
            try(InputStream stream = Files.newInputStream(zebedee.path.resolve(bulletinPath))) {
                Bulletin bulletin = ContentUtil.deserialise(stream, Bulletin.class);

                HashMap<String,String > bulletinDetails = new HashMap<>();
                bulletinDetails.put("Theme", bulletinPath.subpath(1, 2).toString());;
                bulletinDetails.put("Level2", bulletinPath.subpath(2,3).toString());;
                if(bulletinPath.subpath(3,4).toString().equalsIgnoreCase("bulletins")) {
                    bulletinDetails.put("Level3", "");
                    bulletinDetails.put("Title", bulletinPath.subpath(4,5).toString());
                    bulletinDetails.put("DateInUri", bulletinPath.subpath(5,6).toString());
                } else {
                    bulletinDetails.put("Level3", bulletinPath.subpath(3,4).toString());;
                    bulletinDetails.put("Title", bulletinPath.subpath(5,6).toString());;
                    bulletinDetails.put("DateInUri", bulletinPath.subpath(6,7).toString());;
                }
                if (bulletin.getDescription().getReleaseDate() != null) {
                    bulletinDetails.put("ReleaseDate", DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(bulletin.getDescription().getReleaseDate()));
                } else {
                    bulletinDetails.put("ReleaseDate", "");
                }
                bulletinDetails.put("Title", bulletin.getDescription().getTitle());
                bulletinDetails.put("Edition", bulletin.getDescription().getEdition());
                bulletinDetails.put("NextRelease", bulletin.getDescription().getNextRelease());
                bulletinDetails.put("Uri", bulletin.getUri().toString());
                bulletinDetails.put("Path", bulletinPath.toString());
                this.bulletins.add(bulletinDetails);
            }
        }
    }

    private void findArticles() throws IOException {
        List<Path> articles = launchpadMatching(articleMatcher());
        for (Path articlePath: articles) {
            try(InputStream stream = Files.newInputStream(zebedee.path.resolve(articlePath))) {
                Article article = ContentUtil.deserialise(stream, Article.class);

                HashMap<String,String > articleDetails = new HashMap<>();
                articleDetails.put("Theme", articlePath.subpath(1, 2).toString());;
                articleDetails.put("Level2", articlePath.subpath(2,3).toString());;
                if(articlePath.subpath(3,4).toString().equalsIgnoreCase("articles")) {
                    articleDetails.put("Level3", "");
                    articleDetails.put("Title", articlePath.subpath(4,5).toString());
                    articleDetails.put("DateInUri", articlePath.subpath(5,6).toString());
                } else {
                    articleDetails.put("Level3", articlePath.subpath(3,4).toString());;
                    articleDetails.put("TitleInUri", articlePath.subpath(5,6).toString());;
                    articleDetails.put("DateInUri", articlePath.subpath(6,7).toString());;
                }
                if (article.getDescription().getReleaseDate() != null) {
                    articleDetails.put("ReleaseDate", DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(article.getDescription().getReleaseDate()));
                } else {
                    articleDetails.put("ReleaseDate", "");
                }
                articleDetails.put("Title", article.getDescription().getTitle());
                articleDetails.put("Edition", article.getDescription().getEdition());
                articleDetails.put("NextRelease", article.getDescription().getNextRelease());
                articleDetails.put("Uri", article.getUri().toString());
                articleDetails.put("Path", articlePath.toString());
                this.articles.add(articleDetails);
            }
        }
    }

    private void findPages() throws IOException {
        List<Path> pages = launchpadMatching(taxonomyPageMatcher());
        for (Path pagePath: pages) {
            try(InputStream stream = Files.newInputStream(zebedee.path.resolve(pagePath))) {
                ProductPage page = ContentUtil.deserialise(stream, ProductPage.class);

                HashMap<String,String > pageDetails = new HashMap<>();
                if (pagePath.subpath(1, 2).toString().equalsIgnoreCase("data.json")) {
                    pageDetails.put("Theme", "");
                    pageDetails.put("Level2", "");
                    pageDetails.put("Level3", "");
                } else if (pagePath.subpath(2,3).toString().equalsIgnoreCase("data.json")) {
                    pageDetails.put("Theme", pagePath.subpath(1, 2).toString());
                    pageDetails.put("Level2", "");
                    pageDetails.put("Level3", "");
                } else if (pagePath.subpath(3,4).toString().equalsIgnoreCase("data.json")) {
                    pageDetails.put("Theme", pagePath.subpath(1, 2).toString());
                    pageDetails.put("Level2", pagePath.subpath(2,3).toString());
                    pageDetails.put("Level3", "");
                } else {
                    pageDetails.put("Theme", pagePath.subpath(1, 2).toString());
                    pageDetails.put("Level2", pagePath.subpath(2,3).toString());
                    pageDetails.put("Level3", pagePath.subpath(3,4).toString());
                }

                pageDetails.put("Type", page.getType().toString());
                pageDetails.put("Path", pagePath.toString());
                if (page.getUri() != null) {
                    pageDetails.put("Uri", page.getUri().toString());
                } else {
                    pageDetails.put("Uri", "");
                }
                if (page.getDescription() != null && page.getDescription().getTitle() != null) {
                    pageDetails.put("Title", page.getDescription().getTitle());
                } else {
                    pageDetails.put("Title", "");
                }

                this.pages.add(pageDetails);
            }
        }
    }

    private void findDatasets() throws IOException {
        List<Path> datasets = launchpadMatching(dataSetMatcher());
        for (Path datasetPath: datasets) {
            try(InputStream stream = Files.newInputStream(zebedee.path.resolve(datasetPath))) {
                Dataset dataset = ContentUtil.deserialise(stream, Dataset.class);

                HashMap<String,String > datasetDetails = new HashMap<>();
                datasetDetails.put("Theme", datasetPath.subpath(1, 2).toString());;
                datasetDetails.put("Level2", datasetPath.subpath(2,3).toString());;
                if(datasetPath.subpath(3,4).toString().equalsIgnoreCase("datasets")) {
                    datasetDetails.put("Level3", "");
                    datasetDetails.put("TitleInUri", datasetPath.subpath(4,5).toString());
                    datasetDetails.put("DateInUri", datasetPath.subpath(5,6).toString());
                } else {
                    datasetDetails.put("Level3", datasetPath.subpath(3,4).toString());;
                    datasetDetails.put("Title", datasetPath.subpath(5,6).toString());;
                    datasetDetails.put("DateInUri", datasetPath.subpath(6,7).toString());;
                }
                if (dataset.getDescription().getReleaseDate() != null) {
                    datasetDetails.put("ReleaseDate", DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(dataset.getDescription().getReleaseDate()));
                } else {
                    datasetDetails.put("ReleaseDate", "");
                }
                datasetDetails.put("Title", dataset.getDescription().getTitle());
                datasetDetails.put("Edition", dataset.getDescription().getEdition());
                datasetDetails.put("NextRelease", dataset.getDescription().getNextRelease());
                datasetDetails.put("Uri", dataset.getUri().toString());
                datasetDetails.put("Path", datasetPath.toString());
                this.datasets.add(datasetDetails);
            }
        }
    }



    /**
     * Check the specific uri links inside the flat file database
     *
     * @return
     * @throws IOException
     */
    public boolean checkIntegrity() throws IOException {
        checkedUris = 0;
        if (!catalogueMade) { catalogue(); }

        checkBulletinIntegrity();
        checkArticleIntegrity();
        checkDatasetIntegrity();
        checkProductPageIntegrity();

        if (contentErrors.size() == 0) {
            return true;
        } else {
            return false;
        }
    }
    private boolean checkBulletinIntegrity() throws IOException {
        for (HashMap<String, String> bulletinMap : bulletins) {

            Path path = zebedee.launchpad.get(bulletinMap.get("Uri"));
            if(path != null) { path = path.resolve("data.json"); }

            if (path == null || !Files.exists(path)) {
                HashMap<String, String> map = new HashMap<>();
                map.put("type", "bulletin");
                map.put("source", bulletinMap.get("Path"));
                map.put("fault", "broken uri");
                contentErrors.add(map);
            } else {
                try (InputStream stream = Files.newInputStream(path)) {
                    Bulletin bulletin = ContentUtil.deserialise(stream, Bulletin.class);
                    System.out.println("Checking bulletin: " + bulletin.getUri().toString());
                    for (String uri : GraphUtils.relatedUris(bulletin)) {
                        if (zebedee.launchpad.get(uri) == null) {
                            HashMap<String, String> map = new HashMap<>();
                            map.put("type", "bulletin");
                            map.put("source", bulletinMap.get("Uri"));
                            map.put("link", uri);
                            contentErrors.add(map);
                        }
                        checkedUris++;
                    }
                }
            }
        }
        return true;
    }
    private boolean checkArticleIntegrity() throws IOException {
        for (HashMap<String, String> articleMap : articles) {

            Path path = zebedee.launchpad.get(articleMap.get("Uri"));
            if(path != null) { path = path.resolve("data.json"); }

            if (path == null || !Files.exists(path)) {
                HashMap<String, String> map = new HashMap<>();
                map.put("type", "article");
                map.put("source", articleMap.get("Path"));
                map.put("fault", "broken uri");
                contentErrors.add(map);
            } else {
                try (InputStream stream = Files.newInputStream(zebedee.launchpad.get(articleMap.get("Uri")).resolve("data.json"))) {
                    Article article = ContentUtil.deserialise(stream, Article.class);
                    for (String uri : GraphUtils.relatedUris(article)) {
                        if (zebedee.launchpad.get(uri) == null) {
                            HashMap<String, String> map = new HashMap<>();
                            map.put("type", "article");
                            map.put("source", articleMap.get("Uri"));
                            map.put("link", uri);
                            contentErrors.add(map);
                        }
                        checkedUris++;
                    }
                }
            }
        }
        return true;
    }
    private boolean checkDatasetIntegrity() throws IOException {
        for (HashMap<String, String> datasetMap : datasets) {
            try(InputStream stream = Files.newInputStream(zebedee.launchpad.get(datasetMap.get("Uri")).resolve("data.json"))) {
                Dataset dataset = ContentUtil.deserialise(stream, Dataset.class);
                List<String> relatedUris = GraphUtils.relatedUris(dataset);
                for(String uri: relatedUris) {
                    if ((uri == null) || zebedee.launchpad.get(uri) == null) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put("type", "dataset");
                        map.put("source", datasetMap.get("Uri"));
                        map.put("link", uri);
                        contentErrors.add(map);
                    }
                    checkedUris ++;
                }
            }
        }
        return true;
    }
    private boolean checkProductPageIntegrity() throws IOException {
        for (HashMap<String, String> pageMap : pages) {
            try (InputStream stream = Files.newInputStream(zebedee.launchpad.get(pageMap.get("Uri")).resolve("data.json"))) {
                String json = IOUtils.toString(stream);
                Page page = ContentUtil.deserialisePage(json);
                System.out.println(page.getUri().toString());
                if (page.getType() == PageType.product_page) {
                    ProductPage productPage = ContentUtil.deserialise(json, ProductPage.class);
                    for (String uri : GraphUtils.relatedUris(productPage)) {
                        if (zebedee.launchpad.get(uri) == null) {
                            HashMap<String, String> map = new HashMap<>();
                            map.put("type", "product_page");
                            map.put("source", pageMap.get("Uri"));
                            map.put("link", uri);
                            contentErrors.add(map);
                        }
                        checkedUris++;
                    }
                } else {
                    TaxonomyLandingPage taxonomyLandingPage = ContentUtil.deserialise(json, TaxonomyLandingPage.class);
                    for (String uri : GraphUtils.relatedUris(taxonomyLandingPage)) {
                        if (zebedee.launchpad.get(uri) == null) {
                            HashMap<String, String> map = new HashMap<>();
                            map.put("type", "taxonomy_page");
                            map.put("source", pageMap.get("Uri"));
                            map.put("link", uri);
                            contentErrors.add(map);
                        }
                        checkedUris++;
                    }
                }
            }
        }
        return true;
    }


    public boolean validateJSON() throws IOException {
        List<Path> jsonFiles = launchpadMatching(jsonMatcher());
        for (Path file: jsonFiles) {
            try(InputStream stream = Files.newInputStream(zebedee.path.resolve(file))) {
                String json = IOUtils.toString(stream);
                if (!isJSONValid(json)) {
                    invalidJson.add(file.toString());
                }
            }
        }
        if (invalidJson.size() == 0) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isJSONValid(String json) {
        try {
            gson.fromJson(json, Object.class);
            return true;
        } catch(com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }

   // -----------------------------------------------------------------------------------------------------------------


    public List<Path> launchpadMatching(final PathMatcher matcher) throws IOException {
        Path startPath = zebedee.launchpad.path;
        final List<Path> paths = new ArrayList<>();

        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (matcher.matches(file)) {
                    paths.add(zebedee.path.relativize(file));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }

    public static PathMatcher bulletinMatcher() {
        PathMatcher matcher = new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                if (path.toString().contains("data.json") && path.toString().contains("bulletins")) {
                    return true;
                }
                return false;
            }
        };
        return  matcher;
    }
    public static PathMatcher articleMatcher() {
        PathMatcher matcher = new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                if (path.toString().contains("data.json") && path.toString().contains("articles")) {
                    return true;
                }
                return false;
            }
        };
        return  matcher;
    }
    public static PathMatcher taxonomyPageMatcher() {
        PathMatcher matcher = new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                if (path.toString().contains("data.json") && !path.toString().contains("articles") &&
                        !path.toString().contains("dataset") && !path.toString().contains("timeseries") &&
                        !path.toString().contains("bulletin") && !path.toString().contains("releases")) {
                    return true;
                }
                return false;
            }
        };
        return  matcher;
    }
    public static PathMatcher dataSetMatcher() {
        PathMatcher matcher = new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                if (path.toString().contains("data.json") && path.toString().contains("datasets")) {
                    return true;
                }
                return false;
            }
        };
        return  matcher;
    }
    public static PathMatcher jsonMatcher() {
        PathMatcher matcher = new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                if (path.toString().endsWith(".json")) {
                    return true;
                }
                return false;
            }
        };
        return  matcher;
    }

    public static PathMatcher dataDotJsonMatcher() {
        PathMatcher matcher = new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                if (path.toString().endsWith("data.json")) {
                    return true;
                }
                return false;
            }
        };
        return  matcher;
    }
}