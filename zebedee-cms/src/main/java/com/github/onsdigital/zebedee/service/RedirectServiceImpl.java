package com.github.onsdigital.zebedee.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.dis.redirect.api.sdk.RedirectClient;
import com.github.onsdigital.dis.redirect.api.sdk.exception.BadRequestException;
import com.github.onsdigital.dis.redirect.api.sdk.exception.RedirectAPIException;
import com.github.onsdigital.dis.redirect.api.sdk.exception.RedirectNotFoundException;
import com.github.onsdigital.dis.redirect.api.sdk.model.Redirect;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionRedirect;
import com.github.onsdigital.zebedee.json.CollectionRedirectAction;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.ContentReader;

import com.github.onsdigital.zebedee.util.slack.Notifier;
import org.apache.commons.lang.StringUtils;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;

/**
 * Redirect related services
 */
public class RedirectServiceImpl implements RedirectService {

    private RedirectClient redirectClient;

    private Supplier<Zebedee> zebedeeSupplier = () -> Root.zebedee;

    /**
     * Construct a new instance of the the redirect service
     *
     * @param redirectClient An instance of an Redirect API client to be used by the service
     */
    public RedirectServiceImpl(RedirectClient redirectClient) {
        this.redirectClient = redirectClient;
    }

    public void generateRedirectListForCollection(Collection collection, CollectionReader collectionReader)
            throws IOException, ZebedeeException {
        ContentReader reviewedContentReader = collectionReader.getReviewed();

        // Loop through the uri's in the collection
        for (String reviewedUri : reviewedContentReader.listUris()) {

            // Ignoring previous versions loop through the pages
            if (reviewedUri.toLowerCase().endsWith("data.json")) {

                // Strip off data.json
                String pageUri = reviewedUri.substring(0, reviewedUri.length() - "/data.json".length());

                // Find all pages
                Page page = reviewedContentReader.getContent(pageUri);

                String migrationPath = page.getDescription().getMigrationLink();

                if (migrationPath != null) {
                    Redirect redirect = new Redirect(pageUri, migrationPath);
                    CollectionRedirect collectionRedirect = getCollectionRedirect(redirect);
                    if (collectionRedirect.getAction() != CollectionRedirectAction.NO_ACTION) {
                        collection.getDescription().addRedirect(collectionRedirect);
                    }
                }
            }
        }
    }

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
        } catch (BadRequestException | RedirectAPIException | IOException ex) {
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

        for (CollectionRedirect r : redirects) {
            boolean exists = false;
            try {
                redirectClient.getRedirect(r.getFrom());
                exists = true;
            } catch (RedirectNotFoundException nf) {
                exists = false;
            } catch (BadRequestException bre) {
                sendRedirectWarning(collection, r, "Bad request querying Redirect API: " + bre.getMessage(), notifier);
                continue;
            } catch (RedirectAPIException | IOException e) {
                sendRedirectWarning(collection, r, "Error querying Redirect API: " + e.getMessage(), notifier);
                continue;
            }

            if (isValidForAction(r, exists)) {
                validRedirects.add(r);
            } else {
                sendRedirectWarning(
                        collection,
                        r,
                        "State mismatch. Expected for " + r.getAction() + " that redirect "
                                + (r.getAction() == CollectionRedirectAction.CREATE ? "does NOT exist" : "exists")
                                + " but exists=" + exists,
                        notifier
                );
            }
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

    private void sendRedirectWarning(Collection collection, CollectionRedirect r, String reason, Notifier notifier) {
        String msg = String.format(
                "Redirect %s %s -> %s skipped: %s",
                r.getAction(), r.getFrom(), r.getTo(), reason
        );

        notifier.sendCollectionWarning(
                collection,
                Configuration.getDefaultSlackWarningChannel(),
                msg
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
