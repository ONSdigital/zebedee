package com.github.onsdigital.zebedee.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.net.URI;

import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.dis.redirect.api.sdk.RedirectClient;
import com.github.onsdigital.dis.redirect.api.sdk.exception.BadRequestException;
import com.github.onsdigital.dis.redirect.api.sdk.exception.RedirectAPIException;
import com.github.onsdigital.dis.redirect.api.sdk.exception.RedirectNotFoundException;
import com.github.onsdigital.dis.redirect.api.sdk.model.Redirect;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionRedirect;
import com.github.onsdigital.zebedee.json.CollectionRedirectAction;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.ContentReader;

import com.github.onsdigital.zebedee.util.slack.Notifier;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.core5.http.ParseException;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.content.page.base.PageType.ARTICLE;
import static com.github.onsdigital.zebedee.content.page.base.PageType.BULLETIN;
import static com.github.onsdigital.zebedee.content.page.base.PageType.COMPENDIUM_LANDING_PAGE;
/**
 * Redirect related services
 */
public class RedirectServiceImpl implements RedirectService {

    private RedirectClient redirectClient;

    /**
     * These content types are ones that are serialised and therefore need different redirects.
     */
    private static final List<PageType> SERIES_CONTENT_TYPES = Arrays.asList(ARTICLE, BULLETIN, COMPENDIUM_LANDING_PAGE);

    private boolean isSeriesContentType(PageType pageType) { return SERIES_CONTENT_TYPES.contains(pageType);}

    private static final String LATEST_PATH = "/latest";
    private static final String RELATED_DATA_MIGRATION_FROM = "/relateddata";
    private static final String RELATED_DATA_MIGRATION_TO = "/related-data";
    private static final String PREVIOUS_RELEASES_MIGRATION_FROM = "/previousreleases";
    private static final String PREVIOUS_RELEASES_MIGRATION_TO = "/editions";


    /**
     * Construct a new instance of the the redirect service
     *
     * @param redirectClient An instance of an Redirect API client to be used by the service
     */
    public RedirectServiceImpl(RedirectClient redirectClient) {
        this.redirectClient = redirectClient;
    }

    /**
     * createPreviousReleasesRedirect constructs a redirect for the previous releases path
     * @param pageUri
     * @param migrationUri
     * @return Redirect
     */
    private Redirect createPreviousReleasesRedirect(String pageUri, String migrationUri) {
        String parentUri = pageUri.substring(0, pageUri.lastIndexOf('/'));
        String from = String.format("%s%s", parentUri, PREVIOUS_RELEASES_MIGRATION_FROM);
        String to = StringUtils.isBlank(migrationUri) ? "" : String.format("%s%s", migrationUri, PREVIOUS_RELEASES_MIGRATION_TO);

        return new Redirect(from, to);
    }

    /**
     * createRelatedDataRedirect constructs a redirect for the related data path for the 'latest' edition
     * @param pageUri
     * @param migrationUri
     * @return Redirect
     */
    private Redirect createRelatedDataRedirect(String pageUri, String migrationUri) {
        String parentUri = pageUri.substring(0, pageUri.lastIndexOf('/'));
        String from = String.format("%s%s%s", parentUri, LATEST_PATH, RELATED_DATA_MIGRATION_FROM);
        String to = StringUtils.isBlank(migrationUri) ? "" : String.format("%s%s", migrationUri, RELATED_DATA_MIGRATION_TO);

        return new Redirect(from, to);
    }

    /**
     * createLatestRedirect constructs a redirect for the 'latest' edition
     * @param pageUri
     * @param migrationUri
     * @return Redirect
     */
    private Redirect createLatestRedirect(String pageUri, String migrationUri) {
        String parentUri = pageUri.substring(0, pageUri.lastIndexOf('/'));
        String from = String.format("%s%s", parentUri, LATEST_PATH);
        return new Redirect(from, migrationUri);
    }

    /**
     * Takes page data and creates a list of redirects for it.
     * @param page
     * @return
     * @throws ZebedeeException
     */
    private List<CollectionRedirect> createRedirectsForPage(Page page) throws ZebedeeException {
        List<CollectionRedirect> collectionRedirects = new ArrayList<>();

        PageDescription pageDescription = page.getDescription();
        URI pageUri = page.getUri();

        if (pageDescription != null && pageUri != null) {
            String migrationPath = pageDescription.getMigrationLink();
            String pageUriString = pageUri.toString();

            if (migrationPath != null && !migrationPath.equals(pageUriString)) {
                if (isSeriesContentType(page.getType())) {
                    collectionRedirects.add(getCollectionRedirect(createLatestRedirect(pageUriString, migrationPath)));
                    collectionRedirects.add(getCollectionRedirect(createRelatedDataRedirect(pageUriString, migrationPath)));
                    collectionRedirects.add(getCollectionRedirect(createPreviousReleasesRedirect(pageUriString, migrationPath)));
                } else {
                    collectionRedirects.add(getCollectionRedirect(new Redirect(pageUriString, migrationPath)));
                }
            }
        }

        return collectionRedirects;
    }

    /**
     * generateRedirectListForCollection creates a list of redirects for a particular collection
     * @param collection the collection metadata for modifying
     * @param collectionReader a collectionReader with which to get information from the collection 
     * @return void
     */
    public void generateRedirectListForCollection(Collection collection, CollectionReader collectionReader)
            throws IOException, ZebedeeException {
        ContentReader reviewedContentReader = collectionReader.getReviewed();

        // Loop through the uri's in the collection
        for (String reviewedUri : reviewedContentReader.listUris()) {

            if (reviewedUri.toLowerCase().endsWith("data.json")) {

                // Strip off data.json
                String pageUri = reviewedUri.substring(0, reviewedUri.length() - "/data.json".length());

                // Find page content
                Page page = reviewedContentReader.getContent(pageUri);

                List<CollectionRedirect> collectionRedirects = createRedirectsForPage(page);

                collectionRedirects.stream().forEach(collectionRedirect -> {
                    if (collectionRedirect.getAction() != CollectionRedirectAction.NO_ACTION){
                        collection.getDescription().addRedirect(collectionRedirect);
                    }
                });
            }
        }
    }

    /**
     * Gets a collection redirect by querying the proposed redirect with the Redirect API.
     */
    public CollectionRedirect getCollectionRedirect(Redirect redirect) throws ZebedeeException {

        CollectionRedirectAction collectionRedirectAction = CollectionRedirectAction.NO_ACTION;

        try {
            Redirect apiRedirect = redirectClient.getRedirect(redirect.getFrom());

            if (StringUtils.isBlank(redirect.getTo()) && StringUtils.isNotBlank(apiRedirect.getTo())) {
                collectionRedirectAction = CollectionRedirectAction.DELETE;
            } else if (!redirect.getTo().equals(apiRedirect.getTo())) {
                collectionRedirectAction = CollectionRedirectAction.UPDATE;
            }
        } catch (RedirectNotFoundException notFoundException) {
            if (StringUtils.isNotBlank(redirect.getTo())) {
                collectionRedirectAction = CollectionRedirectAction.CREATE;
            }
        } catch (BadRequestException | RedirectAPIException | ParseException | IOException ex) {
            error().exception(ex).log("error communicating with redirect API");
            throw new InternalServerError("couldn't generate redirect from redirect API data");
        }

        return new CollectionRedirect(redirect.getFrom(), redirect.getTo(), collectionRedirectAction);
    }

    private List<CollectionRedirect> validateRedirects(Collection collection, List<CollectionRedirect> redirects, Notifier notifier) {
        if (redirects == null || redirects.isEmpty()) {
            return Collections.emptyList();
        }

        List<CollectionRedirect> validRedirects = new ArrayList<>();

        int validationErrors = 0;

        for (CollectionRedirect r : redirects) {
            boolean exists = false;
            try {
                redirectClient.getRedirect(r.getFrom());
                exists = true;
            } catch (RedirectNotFoundException nf) {
                exists = false;
            } catch (BadRequestException bre) {
                error()
                    .data("message", bre.getMessage())
                    .logException(bre, "bad request quering redirect API");

                validationErrors++;
                continue;
            } catch (RedirectAPIException |  ParseException | IOException e) {
                error()
                    .data("message", e.getMessage())
                    .logException(e, "error querying Redirect API");

                validationErrors++;
                continue;
            }

            if (isValidForAction(r, exists)) {
                validRedirects.add(r);
            } else {
                error()
                    .data("action", r.getAction())
                    .data("from", r.getFrom())
                    .data("to", r.getTo())
                    .data("exists", exists)
                    .log("redirect state mismatch");

                validationErrors++;
            }
        }

        if (validationErrors > 0){
            sendRedirectWarning(
                collection,
                String.format("%d %s validating redirects for collection", validationErrors, validationErrors == 1 ? "error": "errors"),
                notifier
            );
        }

        return validRedirects;
    }

    private void publishRedirects(List<CollectionRedirect> redirects, String collectionId) throws IOException {
        if (redirects == null || redirects.isEmpty()) {
            return;
        }

        List<String> failures = new ArrayList<>();

        for (CollectionRedirect r : redirects) {
            if (r == null || r.getAction() == null || r.getAction() == CollectionRedirectAction.NO_ACTION) {
                continue;
            }

            final String from = normaliseFrom(r.getFrom());
            final String to = r.getTo();

            try {
                switch (r.getAction()) {
                    case CREATE:
                    case UPDATE:
                        redirectClient.putRedirect(new Redirect(from, to));
                        break;

                    case DELETE:
                        try {
                            redirectClient.deleteRedirect(from);
                        } catch (RedirectAPIException e) {
                            // Idempotent delete: treat 404 as success, fail otherwise
                            int status = e.getCode();
                            if (status != 404) {
                                throw e;
                            }
                        }
                        break;

                    default:
                        break;
                }
            } catch (RedirectAPIException | IOException e) {
                failures.add(summary(r, from, e.getMessage()));
            }
        }

        if (!failures.isEmpty()) {
            throw new IOException("Redirect operations failed: " + String.join(" | ", failures));
        }
    }

    @Override
    public void publishRedirectsForCollection(Collection collection, Notifier notifier) throws IOException {
        List<CollectionRedirect> redirects = collection.getDescription().getRedirects();
        List<CollectionRedirect> validRedirects = validateRedirects(collection, redirects, notifier);
        publishRedirects(validRedirects, collection.getId());
    }

    private boolean isValidForAction(CollectionRedirect r, boolean exists) {
        switch (r.getAction()) {
            case CREATE:
                return !exists;
            case UPDATE:
            case DELETE:
                return exists;
            default:
                return false;
        }
    }

    private void sendRedirectWarning(Collection collection, String reason, Notifier notifier) {
        notifier.sendCollectionWarning(
                collection,
                Configuration.getDefaultSlackWarningChannel(),
                reason
        );
    }


    private static String normaliseFrom(String from) {
        if (from == null || from.isEmpty()) return "/";
        return from.startsWith("/") ? from : "/" + from;
    }

    private static String summary(CollectionRedirect r, String from, String msg) {
        return r.getAction() + " " + from + " -> " + r.getTo() + " : " + msg;
    }

}
