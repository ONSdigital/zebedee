package com.github.onsdigital.zebedee.exceptions;

import javax.ws.rs.core.Response;
import java.text.MessageFormat;

/**
 * Created by dave on 4/19/16.
 */
public class TableBuilderException extends ZebedeeException {

    public enum ErrorType {

        NUMBER_PARSE_ERROR("Invalid row number \"{0}\"."),

        NEGATIVE_ROW_INDEX("Excluded row numbers must be greater than or equal to 0."),

        ROW_INDEX_OUT_OF_BOUNDS("Invalid row index: {0}. The max row index for this file is: {1}."),

        UNEXPECTED_ERROR("Unexpected error occurred: {0}"),

        INVALID_JSON_ERROR("Unexpected error while attempting to deserialise JSON: {0}");

        private final String message;

        ErrorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return this.message;
        }
    }

    public TableBuilderException(ErrorType errorType, Object... args) {
        super(MessageFormat.format(errorType.getMessage(), args),
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
}
