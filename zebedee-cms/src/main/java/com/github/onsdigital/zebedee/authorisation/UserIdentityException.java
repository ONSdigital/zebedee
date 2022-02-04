package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @deprecated The AuthorisationService is deprecated in favour of the new JWT sessions. Validating the JWT signature
 *             accomplishes the same functionality as this implementation, but in a more distributed fashion.
 *
 * TODO: Once the migration to JWT sessions has been completed and all microservices have been updated to use the new
 *       dp-authorisation implementation that includes JWT validation, then this service should be removed
 */
@Deprecated
public class UserIdentityException extends ZebedeeException {

    public UserIdentityException(String message, int responseCode) {
        super(message, responseCode);
    }

    public int getResponseCode() {
        return super.statusCode;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
