package com.github.onsdigital.zebedee.util;

import au.com.bytecode.opencsv.CSVWriter;
import com.github.onsdigital.content.link.PageReference;
import com.github.onsdigital.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.content.page.statistics.document.article.Article;
import com.github.onsdigital.content.page.statistics.document.bulletin.Bulletin;

import com.github.onsdigital.content.page.taxonomy.ProductPage;
import com.github.onsdigital.content.page.taxonomy.TaxonomyLandingPage;
import com.github.onsdigital.content.util.ContentUtil;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.util.*;

/**
 * Created by thomasridd on 06/07/15.
 */
public class Librarian {
    Zebedee zebedee;

    List<HashMap<String, String>> bulletins = new ArrayList<>();
    List<HashMap<String, String>> articles = new ArrayList<>();
    List<HashMap<String, String>> pages = new ArrayList<>();
    List<HashMap<String, String>> datasets = new ArrayList<>();

    List<HashMap<String, String>> brokenLinks = new ArrayList<>();
    List<HashMap<String, String>> falseUris = new ArrayList<>();

    public Librarian(Zebedee zebedee) {
        this.zebedee = zebedee;
    }

    public void catalogue() throws IOException {

        findBulletins();
        findArticles();
        findPages();
        findDatasets();

    }

    public List<String> getFilesThatLinkToURI(String uri) throws IOException {

        List<String> matches = new ArrayList<>();

        List<Path> paths = launchpadMatching(bulletinMatcher());
        for (Path bulletinPath: paths) {
            Bulletin bulletin;
            try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(bulletinPath))) {
                bulletin = ContentUtil.deserialise(inputStream, Bulletin.class);
            }
            for (PageReference reference: bulletin.getRelatedBulletins()) {
                if (reference.getUri() != null && reference.getUri().toString().equalsIgnoreCase(uri)) {
                    matches.add(bulletin.getUri().toString());
                }
            }
            for (PageReference reference: bulletin.getRelatedData()) {
                if (reference.getUri() != null && reference.getUri().toString().equalsIgnoreCase(uri)) {
                    matches.add(bulletin.getUri().toString());
                }
            }
        }

        paths = launchpadMatching(articleMatcher());
        for (Path articlePath: paths) {
            Article article;
            try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(articlePath))) {
                article = ContentUtil.deserialise(inputStream, Article.class);
            }
            if (article.getRelatedArticles() != null) {
                for (PageReference reference : article.getRelatedArticles()) {
                    if (reference.getUri() != null && reference.getUri().toString().equalsIgnoreCase(uri)) {
                        matches.add(article.getUri().toString());
                    }
                }
            }
            if (article.getRelatedData() != null) {
                for (PageReference reference : article.getRelatedData()) {
                    if (reference.getUri() != null && reference.getUri().toString().equalsIgnoreCase(uri)) {
                        matches.add(article.getUri().toString());
                    }
                }
            }
        }

        paths = launchpadMatching(pageMatcher());
        for (Path taxonomyPath: paths) {
            ProductPage page;
            try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(taxonomyPath))) {
                page = ContentUtil.deserialise(inputStream, ProductPage.class);
            }

            if (page.getRelatedArticles() != null) {
                for (PageReference reference : page.getRelatedArticles()) {
                    if (reference.getUri() != null && reference.getUri().toString().equalsIgnoreCase(uri)) {
                        matches.add(page.getUri().toString());
                    }
                }
            }

            if (page.getDatasets() != null) {
                for (PageReference reference : page.getStatsBulletins()) {
                    if (reference.getUri() != null && reference.getUri().toString().equalsIgnoreCase(uri)) {
                        matches.add(page.getUri().toString());
                    }
                }
            }

            if (page.getItems() != null) {
                for (PageReference reference : page.getItems()) {
                    if (reference.getUri() != null && reference.getUri().toString().equalsIgnoreCase(uri)) {
                        matches.add(page.getUri().toString());
                    }
                }
            }

            if (page.getStatsBulletins() != null) {
                for (PageReference reference : page.getStatsBulletins()) {
                    if (reference.getUri() != null && reference.getUri().toString().equalsIgnoreCase(uri)) {
                        matches.add(page.getUri().toString());
                    }
                }
            }
        }

        paths = launchpadMatching(pageMatcher());
        for (Path taxonomyPath: paths) {
            TaxonomyLandingPage page;
            try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(taxonomyPath))) {
                page = ContentUtil.deserialise(inputStream, TaxonomyLandingPage.class);
            }

            if (page.getSections() != null) {
                for (PageReference reference : page.getSections()) {
                    if (reference.getUri() != null && reference.getUri().toString().equalsIgnoreCase(uri)) {
                        matches.add(page.getUri().toString());
                    }
                }
            }
        }

        return matches;
    }

    /**
     * Get all bulletins and list them
     * 
     * @throws IOException
     */
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
                this.bulletins.add(bulletinDetails);
            }
        }
    }

    /**
     * Get all articles and list them
     *
     * @throws IOException
     */
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
                this.articles.add(articleDetails);
            }
        }
    }

    /**
     * Get all T2 and T3s and list them
     * 
     * @throws IOException
     */
    private void findPages() throws IOException {
        List<Path> pages = launchpadMatching(pageMatcher());
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
                this.datasets.add(datasetDetails);
            }
        }
    }

    private void findBrokenLinks() {

    }


    public Path csvOfUnfoundLinks(Path uriPath) throws IOException {
        Path path = Files.createTempFile("unfoundlinks", ".csv");
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(Files.newOutputStream(path), Charset.forName("UTF8")), ',')) {

            String[] row;
            row = new String[10];
            row[0] = "Data type";
            row[1] = "My URI";
            row[2] = "Related URI";
            writer.writeNext(row);

            writeCSVForUnfoundLinks(uriPath, writer);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }
    void writeCSVForUnfoundLinks(Path path, CSVWriter writer) throws IOException {
        List<Path> paths = launchpadMatching(bulletinMatcher());
        String[] row;
        row = new String[10];
        row[0] = "Data type";
        row[1] = "My URI";
        row[2] = "Related URI";

        for (Path bulletinPath: paths) {
            Bulletin bulletin;
            try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(bulletinPath))) {
                bulletin = ContentUtil.deserialise(inputStream, Bulletin.class);
            }
            for (PageReference reference: bulletin.getRelatedBulletins()) {
                String string = reference.getUri().toString();
                if (string.startsWith("/")) { string = string.substring(1); }
                Path checkpath = zebedee.published.path.resolve(string);
                if (Files.exists(checkpath) == false) {
                    row[0] = "Related bulletin";
                    row[1] = bulletin.getUri().toString();
                    row[2] = reference.getUri().toString();
                    writer.writeNext(row);
                }
            }
            for (PageReference reference: bulletin.getRelatedData()) {
                String string = reference.getUri().toString();
                if (string.startsWith("/")) { string = string.substring(1); }
                Path checkpath = zebedee.published.path.resolve(string);
                if (Files.exists(checkpath) == false) {
                    row[0] = "Related data";
                    row[1] = bulletin.getUri().toString();
                    row[2] = reference.getUri().toString();
                    writer.writeNext(row);
                }
            }
        }

        paths = launchpadMatching(articleMatcher());
        for (Path articlePath: paths) {
            Article article;
            try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(articlePath))) {
                article = ContentUtil.deserialise(inputStream, Article.class);
            }
            if (article.getRelatedArticles() != null) {
                for (PageReference reference : article.getRelatedArticles()) {
                    String string = reference.getUri().toString();
                    if (string.startsWith("/")) { string = string.substring(1); }
                    Path checkpath = zebedee.published.path.resolve(string);
                    if (Files.exists(checkpath) == false) {
                        row[0] = "Related article";
                        row[1] = article.getUri().toString();
                        row[2] = reference.getUri().toString();
                        writer.writeNext(row);
                    }
                }
            }
            if (article.getRelatedData() != null) {
                for (PageReference reference : article.getRelatedData()) {
                    String string = reference.getUri().toString();
                    if (string.startsWith("/")) { string = string.substring(1); }
                    Path checkpath = zebedee.published.path.resolve(string);
                    if (Files.exists(checkpath) == false) {
                        row[0] = "Related data";
                        row[1] = article.getUri().toString();
                        row[2] = reference.getUri().toString();
                        writer.writeNext(row);
                    }
                }
            }
        }

        paths = launchpadMatching(pageMatcher());
        for (Path taxonomyPath: paths) {
            ProductPage page;
            try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(taxonomyPath))) {
                page = ContentUtil.deserialise(inputStream, ProductPage.class);
            }

            if (page.getRelatedArticles() != null) {
                for (PageReference reference : page.getRelatedArticles()) {
                    String string = reference.getUri().toString();
                    if (string.startsWith("/")) { string = string.substring(1); }
                    Path checkpath = zebedee.published.path.resolve(string);
                    if (Files.exists(checkpath) == false) {
                        row[0] = "Related article";
                        row[1] = page.getUri().toString();
                        row[2] = reference.getUri().toString();
                        writer.writeNext(row);
                    }
                }
            }

            if (page.getDatasets() != null) {
                for (PageReference reference : page.getStatsBulletins()) {
                    String string = reference.getUri().toString();
                    if (string.startsWith("/")) { string = string.substring(1); }
                    Path checkpath = zebedee.published.path.resolve(string);
                    if (Files.exists(checkpath) == false) {
                        row[0] = "Related bulletin";
                        row[1] = page.getUri().toString();
                        row[2] = reference.getUri().toString();
                        writer.writeNext(row);
                    }
                }
            }

            if (page.getItems() != null) {
                for (PageReference reference : page.getItems()) {
                    String string = reference.getUri().toString();
                    if (string.startsWith("/")) { string = string.substring(1); }
                    Path checkpath = zebedee.published.path.resolve(string);
                    if (Files.exists(checkpath) == false) {
                        row[0] = "Related item";
                        row[1] = page.getUri().toString();
                        row[2] = reference.getUri().toString();
                        writer.writeNext(row);
                    }
                }
            }

            if (page.getStatsBulletins() != null) {
                for (PageReference reference : page.getStatsBulletins()) {
                    String string = reference.getUri().toString();
                    if (string.startsWith("/")) { string = string.substring(1); }
                    Path checkpath = zebedee.published.path.resolve(string);
                    if (string.contains("BLAH")) {
                        System.out.println("BLAH");
                    }
                    if (Files.exists(checkpath) == false) {
                        row[0] = "Related dataset";
                        row[1] = page.getUri().toString();
                        row[2] = reference.getUri().toString();
                        writer.writeNext(row);
                    }
                }
            }
        }

        paths = launchpadMatching(pageMatcher());
        for (Path taxonomyPath: paths) {
            TaxonomyLandingPage page;
            try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(taxonomyPath))) {
                page = ContentUtil.deserialise(inputStream, TaxonomyLandingPage.class);
            }

            if (page.getSections() != null) {
                for (PageReference reference : page.getSections()) {
                    if (reference.getUri() == null) {
                        row[0] = "Related section";
                        row[1] = page.getUri().toString();
                        row[2] = "URI null";
                    } else {
                        String string = reference.getUri().toString();
                        if (string.startsWith("/")) {
                            string = string.substring(1);
                        }
                        Path checkpath = zebedee.published.path.resolve(string);
                        if (Files.exists(checkpath) == false) {
                            row[0] = "Related section";
                            row[1] = page.getUri().toString();
                            row[2] = reference.getUri().toString();
                            writer.writeNext(row);
                        }
                    }
                }
            }
        }
    }

    public Path csvOfFalseURIs() throws IOException {
        Path path = Files.createTempFile("bulletins", ".csv");
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(Files.newOutputStream(path), Charset.forName("UTF8")), ',')) {

            String[] row;
            row = new String[10];
            row[0] = "Theme";
            row[1] = "Level2";
            row[2] = "Level3";
            row[3] = "Actual file path";
            row[4] = "Internal URI";
            writer.writeNext(row);

            writeFalseURIsToCSV(writer);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;

    }

    public void writeFalseURIsToCSV(CSVWriter writer) throws IOException {

        List<Path> paths = launchpadMatching(bulletinMatcher());
        String[] row;
        row = new String[10];
        row[0] = "Theme";
        row[1] = "Level2";
        row[2] = "Level3";
        row[3] = "Actual file path";
        row[4] = "Internal URI";

        for (Path bulletinPath: paths) {
            Bulletin bulletin;
            try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(bulletinPath))) {
                bulletin = ContentUtil.deserialise(inputStream, Bulletin.class);
            }
            String intendedURI = "/" + bulletinPath.subpath(1, bulletinPath.getNameCount() - 1).toString();
            if (!bulletin.getUri().toString().equalsIgnoreCase(intendedURI)) {
                row[0] = bulletinPath.subpath(1,2).toString();
                row[1] = bulletinPath.subpath(2,3).toString();
                row[2] = bulletinPath.subpath(3,4).toString();
                if (row[2].equalsIgnoreCase("bulletins")) {
                    row[2] = "";
                }
                row[3] = bulletinPath.toString();
                row[4] = bulletin.getUri().toString();
                writer.writeNext(row);
            }

        }

        paths = launchpadMatching(articleMatcher());
        for (Path articlePath: paths) {
            Article article;
            try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(articlePath))) {
                article = ContentUtil.deserialise(inputStream, Article.class);
            }

            String intendedURI = "/" + articlePath.subpath(1, articlePath.getNameCount() - 1).toString();
            if (!article.getUri().toString().equalsIgnoreCase(intendedURI)) {
                row[0] = articlePath.subpath(1,2).toString();
                row[1] = articlePath.subpath(2,3).toString();
                row[2] = articlePath.subpath(3,4).toString();
                if (row[2].equalsIgnoreCase("articles")) {
                    row[2] = "";
                }
                row[3] = articlePath.toString();
                row[4] = article.getUri().toString();
                writer.writeNext(row);
            }

        }

        paths = launchpadMatching(pageMatcher());
        for (Path taxonomyPath: paths) {
            ProductPage page;
            try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(taxonomyPath))) {
                page = ContentUtil.deserialise(inputStream, ProductPage.class);
            }

            String intendedURI = (taxonomyPath.getNameCount() <= 2) ? "/" : "/" + taxonomyPath.subpath(1, taxonomyPath.getNameCount() - 1).toString();
            if (!page.getUri().toString().equalsIgnoreCase(intendedURI)) {

                if (taxonomyPath.subpath(1, 2).toString().equalsIgnoreCase("data.json")) {
                    row[0] = "";
                    row[1] = "";
                    row[2] = "";
                } else if (taxonomyPath.subpath(2, 3).toString().equalsIgnoreCase("data.json")) {
                    row[0] = taxonomyPath.subpath(1, 2).toString();
                    row[1] = "";
                    row[2] = "";
                } else if (taxonomyPath.subpath(3, 4).toString().equalsIgnoreCase("data.json")) {
                    row[0] = taxonomyPath.subpath(1, 2).toString();
                    row[1] = taxonomyPath.subpath(2, 3).toString();
                    row[2] = "";
                } else {
                    row[0] = taxonomyPath.subpath(1, 2).toString();
                    row[1] = taxonomyPath.subpath(2, 3).toString();
                    row[2] = taxonomyPath.subpath(3, 4).toString();
                }
                row[3] = taxonomyPath.toString();
                if (page.getUri() != null) {
                    row[4] = page.getUri().toString();
                } else {
                    row[4] = "";
                }
                writer.writeNext(row);
            }
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    public void findIncorrectInternalUris() {

        for (HashMap<String, String> dict: bulletins) {
            
        }
    }

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
    public static PathMatcher pageMatcher() {
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
    public static PathMatcher timeSeriesMatcher() {
        PathMatcher matcher = new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                if (path.toString().contains("data.json") && path.toString().contains("timeseries")) {
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
    public static PathMatcher productPageMatcher() {
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

    public static void main(String[] args) {
        Librarian librarian = new Librarian(Root.zebedee);
    }
}