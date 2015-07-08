package com.github.onsdigital.zebedee.util;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.github.onsdigital.content.link.PageReference;
import com.github.onsdigital.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.content.page.statistics.document.article.Article;
import com.github.onsdigital.content.page.statistics.document.bulletin.Bulletin;

import com.github.onsdigital.content.page.taxonomy.ProductPage;
import com.github.onsdigital.content.page.taxonomy.TaxonomyLandingPage;
import com.github.onsdigital.content.partial.TimeseriesValue;
import com.github.onsdigital.content.util.ContentUtil;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.data.json.TimeseriesPage;
import com.github.onsdigital.zebedee.model.Content;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.mockito.exceptions.verification.VerificationInOrderFailure;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.util.*;

/**
 * Created by thomasridd on 06/07/15.
 */
public class Validator {
    Zebedee zebedee;

    public Validator(Zebedee zebedee) {
        this.zebedee = zebedee;
    }

    public void validate(Path path) throws IOException {

        Path bulletins = csvOfBulletinData();
        Files.deleteIfExists(path.resolve("bulletins.csv"));
        Files.copy(bulletins, path.resolve("bulletins.csv"));


        Path articles = csvOfArticleData();
        Files.deleteIfExists(path.resolve("articles.csv"));
        Files.copy(articles, path.resolve("articles.csv"));


        Path pages = csvOfPagesData();
        Files.deleteIfExists(path.resolve("pages.csv"));
        Files.copy(pages, path.resolve("pages.csv"));

        Path invalidURIs = csvOfFalseURIs();
        Files.deleteIfExists(path.resolve("falseURIs.csv"));
        Files.copy(invalidURIs, path.resolve("falseURIs.csv"));

        Path unfoundLinks = csvOfUnfoundLinks(path);
        Files.deleteIfExists(path.resolve("brokenLinks.csv"));
        Files.copy(unfoundLinks, path.resolve("brokenLinks.csv"));
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

    public Path csvOfArticleData() throws IOException {
        Path path = Files.createTempFile("articles", ".csv");
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(Files.newOutputStream(path), Charset.forName("UTF8")), ',')) {

            String[] row;
            row = new String[10];
            row[0] = "Theme";
            row[1] = "Level2";
            row[2] = "Level3";
            row[3] = "Title";
            row[4] = "URI Date";
            row[5] = "ReleaseDate";
            row[6] = "Title";
            row[7] = "Edition";
            row[8] = "Next Release";

            writer.writeNext(row);

            List<Path> paths = launchpadMatching(articleMatcher());

            for (Path articlePath: paths) {
                Article article;
                try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(articlePath))) {
                    article = ContentUtil.deserialise(inputStream, Article.class);
                }

                row[0] = articlePath.subpath(1,2).toString();;
                row[1] = articlePath.subpath(2,3).toString();;
                if(articlePath.subpath(3,4).toString().equalsIgnoreCase("articles")) {
                    row[2] = "";
                    row[3] = articlePath.subpath(4,5).toString();
                    row[4] = articlePath.subpath(5,6).toString();
                } else {
                    row[2] = articlePath.subpath(3,4).toString();;
                    row[3] = articlePath.subpath(5,6).toString();;
                    row[4] = articlePath.subpath(6,7).toString();;
                }
                if (article.getDescription().getReleaseDate() != null) {
                    row[5] = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(article.getDescription().getReleaseDate());
                } else {
                    row[5] = "";
                }
                row[6] = article.getDescription().getTitle();
                row[7] = article.getDescription().getEdition();
                row[8] = article.getDescription().getNextRelease();
                writer.writeNext(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    public Path csvOfBulletinData() throws IOException {
        Path path = Files.createTempFile("bulletins", ".csv");
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(Files.newOutputStream(path), Charset.forName("UTF8")), ',')) {

            String[] row;
            row = new String[10];
            row[0] = "Theme";
            row[1] = "Level2";
            row[2] = "Level3";
            row[3] = "Title";
            row[4] = "URI Date";
            row[5] = "ReleaseDate";
            row[6] = "Title";
            row[7] = "Edition";
            row[8] = "Next Release";
            writer.writeNext(row);

            List<Path> paths = launchpadMatching(bulletinMatcher());

            for (Path bulletinPath: paths) {
                Bulletin bulletin;
                try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(bulletinPath))) {
                    bulletin = ContentUtil.deserialise(inputStream, Bulletin.class);
                }

                row[0] = bulletinPath.subpath(1,2).toString();;
                row[1] = bulletinPath.subpath(2,3).toString();;
                if(bulletinPath.subpath(3,4).toString().equalsIgnoreCase("bulletins")) {
                    row[2] = "";
                    row[3] = bulletinPath.subpath(4,5).toString();
                    row[4] = bulletinPath.subpath(5,6).toString();
                } else {
                    row[2] = bulletinPath.subpath(3,4).toString();;
                    row[3] = bulletinPath.subpath(5,6).toString();;
                    row[4] = bulletinPath.subpath(6,7).toString();;
                }
                if (bulletin.getDescription().getReleaseDate() != null) {
                    row[5] = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(bulletin.getDescription().getReleaseDate());
                } else {
                    row[5] = "";
                }
                row[6] = bulletin.getDescription().getTitle();
                row[7] = bulletin.getDescription().getEdition();
                row[8] = bulletin.getDescription().getNextRelease();
                writer.writeNext(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    public Path csvOfPagesData() throws IOException {
        Path path = Files.createTempFile("articles", ".csv");
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(Files.newOutputStream(path), Charset.forName("UTF8")), ',')) {

            String[] row;
            row = new String[10];
            row[0] = "Theme";
            row[1] = "Level2";
            row[2] = "Level3";
            row[3] = "Type";
            row[4] = "URI";
            row[5] = "Title";

            writer.writeNext(row);

            List<Path> paths = launchpadMatching(pageMatcher());

            for (Path taxonomyPath: paths) {

                try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(taxonomyPath))) {
                    ProductPage page;
                    page = ContentUtil.deserialise(inputStream, ProductPage.class);
                    if (taxonomyPath.subpath(1, 2).toString().equalsIgnoreCase("data.json")) {
                        row[0] = "";
                        row[1] = "";
                        row[2] = "";
                    } else if (taxonomyPath.subpath(2,3).toString().equalsIgnoreCase("data.json")) {
                        row[0] = taxonomyPath.subpath(1, 2).toString();
                        row[1] = "";
                        row[2] = "";
                    } else if (taxonomyPath.subpath(3,4).toString().equalsIgnoreCase("data.json")) {
                        row[0] = taxonomyPath.subpath(1, 2).toString();
                        row[1] = taxonomyPath.subpath(2,3).toString();
                        row[2] = "";
                    } else {
                        row[0] = taxonomyPath.subpath(1, 2).toString();
                        row[1] = taxonomyPath.subpath(2,3).toString();
                        row[2] = taxonomyPath.subpath(3,4).toString();
                    }

                    row[3] = page.getType().toString();
                    if (page.getUri() != null) {
                        row[4] = page.getUri().toString();
                    } else {
                        row[4] = "";
                    }
                    if (page.getDescription() != null && page.getDescription().getTitle() != null) {
                        row[5] = page.getDescription().getTitle();
                    } else {
                        row[5] = "";
                    }

                    writer.writeNext(row);
                } catch(Exception e) {
                    System.out.println("Could not deserialise page at: " + taxonomyPath);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
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

    public List<Path> filesMatching(final PathMatcher matcher) throws IOException {
        Path startPath = zebedee.published.path;
        final List<Path> paths = new ArrayList<>();

        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException
            {
                if (matcher.matches(file)) {
                    paths.add(zebedee.path.relativize(file));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }

    public List<Path> launchpadMatching(final PathMatcher matcher) throws IOException {
        Path startPath = zebedee.launchpad.path;
        final List<Path> paths = new ArrayList<>();

        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException
            {
                if (matcher.matches(file)) {
                    paths.add(zebedee.path.relativize(file));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }

    public List<Bulletin> bulletinList() throws IOException {
        List<Path> paths = launchpadMatching(bulletinMatcher());

        List<Bulletin> bulletins = new ArrayList<>();
        for (Path path: paths) {
            try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(path))) {
                bulletins.add(ContentUtil.deserialise(inputStream, Bulletin.class));
            }
        }
        return bulletins;
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
        Validator validator = new Validator(Root.zebedee);
    }
}