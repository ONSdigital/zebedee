package com.github.onsdigital.zebedee.service;

import java.io.IOException;

import com.github.onsdigital.dis.redirect.api.sdk.RedirectClient;
import com.github.onsdigital.dis.redirect.api.sdk.exception.BadRequestException;
import com.github.onsdigital.dis.redirect.api.sdk.exception.RedirectAPIException;
import com.github.onsdigital.dis.redirect.api.sdk.exception.RedirectNotFoundException;
import com.github.onsdigital.dis.redirect.api.sdk.model.Redirect;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionRedirect;
import com.github.onsdigital.zebedee.json.CollectionRedirectAction;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.ContentReader;

import org.apache.commons.lang.StringUtils;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;

/**
 * Redirect related services
 */
public class RedirectServiceImpl implements RedirectService {

    private RedirectClient redirectClient;

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
                        if (collectionRedirect.getAction() != CollectionRedirectAction.NO_ACTION){
                            System.out.println(collectionRedirect.getFrom());
                            System.out.println(collectionRedirect.getAction());

                            System.out.println(collectionRedirect.getTo());

                            collection.getDescription().addRedirect(collectionRedirect);
                        }
                    }
                }
            }
        }

    public CollectionRedirect getCollectionRedirect(Redirect redirect) throws ZebedeeException {

        System.out.println("I'm actually running this function");
        CollectionRedirectAction collectionRedirectAction = CollectionRedirectAction.NO_ACTION;

        try {
            Redirect apiRedirect = redirectClient.getRedirect(redirect.getFrom());

            if (StringUtils.isBlank(redirect.getTo()) && StringUtils.isNotBlank(apiRedirect.getTo())) {
                collectionRedirectAction = CollectionRedirectAction.DELETE;
            } else if (!redirect.getTo().equals(apiRedirect.getTo()) ){
                collectionRedirectAction = CollectionRedirectAction.UPDATE;
            }
        } catch (RedirectNotFoundException notFoundException){
            if (StringUtils.isNotBlank(redirect.getTo())){
                collectionRedirectAction = CollectionRedirectAction.CREATE;
            }
        } catch (BadRequestException | RedirectAPIException | IOException ex) {
            error().exception(ex).log("error communicating with redirect API");
            throw new InternalServerError("couldn't generate redirect from redirect API data");
        }

        return new CollectionRedirect(redirect.getFrom(), redirect.getTo(), collectionRedirectAction);
    }
}
