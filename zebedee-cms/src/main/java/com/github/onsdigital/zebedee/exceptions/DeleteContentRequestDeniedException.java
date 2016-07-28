package com.github.onsdigital.zebedee.exceptions;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.json.DeleteMarkerJson;
import com.github.onsdigital.zebedee.model.Collection;
import org.apache.http.HttpStatus;

import java.util.Arrays;

import static java.text.MessageFormat.format;

/**
 * {@link ZebedeeException} implementation for Errors while attempting to add a
 * {@link DeleteMarkerJson}.
 */
public class DeleteContentRequestDeniedException extends ZebedeeException {

    private static final String MARKED_BY_ANOTHER_COLLECTION
        = "Collection: <strong>{1}</strong> has a delete lock on resource: <strong>{0}</strong>";

    private static final String BEING_EDITED_BY_ANOTHER_COLLECTION
            = "Collection: <strong>{1}</strong> has an edit lock on resource: <strong>{0}</strong>";

    private static final String ALREADY_MARKED_BY_THIS_COLLECTION
            = "Resource is already marked for delete in this collection.";

    private static final String PAGE_TYPE_CANNOT_BE_DELETED
            = "Delete not allowed for {0}.";

    /**
     * Delete not allowed for this page type.
     */
    public static DeleteContentRequestDeniedException deleteForbiddenForPageTypeError(PageType pageType) {
        return new DeleteContentRequestDeniedException(PAGE_TYPE_CANNOT_BE_DELETED,
                HttpStatus.SC_FORBIDDEN, pageType.getDisplayName());
    }

    /**
     * Delete not allowed as content is currently being edited in this collection.
     */
    public static DeleteContentRequestDeniedException beingEditedByAnotherCollectionError(
            Collection collection, String pageTitle) {
        return new DeleteContentRequestDeniedException(BEING_EDITED_BY_ANOTHER_COLLECTION,
                HttpStatus.SC_BAD_REQUEST, pageTitle, collection.description.name);
    }

    /**
     * Delete not allowed as content is already marked for delete in this collection.
     */
    public static DeleteContentRequestDeniedException alreadyMarkedDeleteInCurrentCollectionError(
            Collection collection, String uri) {
        return new DeleteContentRequestDeniedException(ALREADY_MARKED_BY_THIS_COLLECTION,
                HttpStatus.SC_BAD_REQUEST, collection.description.name);
    }

    /**
     * Delete not allowed as content is currently being edited in another collection.
     */
    public static DeleteContentRequestDeniedException markedDeleteInAnotherCollectionError(
            Collection collection, String uri) {
        return new DeleteContentRequestDeniedException(MARKED_BY_ANOTHER_COLLECTION,
                HttpStatus.SC_BAD_REQUEST, uri, collection.description.name);
    }

    private static String buildDetailedMessage(String reason, Object... args) {
        if (Arrays.asList(args).isEmpty()) {
            return reason;
        }
        return format(reason, args);
    }

    private DeleteContentRequestDeniedException(String message, int status, Object... args) {
        super(buildDetailedMessage(message, args), status);
    }
}
