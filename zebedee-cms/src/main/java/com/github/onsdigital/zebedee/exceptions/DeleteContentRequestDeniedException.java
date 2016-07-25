package com.github.onsdigital.zebedee.exceptions;

import com.github.onsdigital.zebedee.json.DeleteMarkerJson;
import com.github.onsdigital.zebedee.model.Collection;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import static java.text.MessageFormat.format;

/**
 * {@link ZebedeeException} implementation for Errors while attempting to add a
 * {@link DeleteMarkerJson}.
 */
public class DeleteContentRequestDeniedException extends ZebedeeException {

    /**
     * Type representing the different reasons for the exception to be thrown.
     */
    public enum BlockReason {

        MARKED_BY_ANOTHER_COLLECTION("Resource is currently marked deleted in Collection: {0}."),

        BEING_EDITED_BY_ANOTHER_COLLECTION("Resource is currently being edited in Collection: {0}."),

        ALREADY_MARKED_BY_THIS_COLLECTION("Resource is already marked for delete in this collection.");

        private final String description;

        BlockReason(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }
    }

    public DeleteContentRequestDeniedException(Collection blockingCollection, BlockReason reason) {
        super(buildDetailedMessage(reason, blockingCollection), HttpResponseStatus.BAD_REQUEST.getCode());
    }

    private static String buildDetailedMessage(BlockReason reason, Collection collection) {
        return format(reason.description, collection.description.name);
    }
}
