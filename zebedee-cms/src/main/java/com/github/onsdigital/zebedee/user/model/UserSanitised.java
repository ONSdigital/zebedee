package com.github.onsdigital.zebedee.user.model;

import com.github.onsdigital.zebedee.json.AdminOptions;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represents a reduced view of user account, suitable for sending to clients via the API.
 * NB this record intentionally does not contain any authentication, encryption or permission-related information.
 * This is purely acconut information.
 */
public class UserSanitised {

    protected String name;
    protected String email;

    /**
     * This field is {@link Boolean} rather than <code>boolean</code> so that it can be <code>null</code> in an update message.
     * This ensures the value won't change unless explicitly specified.
     */
    protected Boolean inactive;
    protected Boolean temporaryPassword;
    protected String lastAdmin;
    protected AdminOptions adminOptions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getInactive() {
        return inactive;
    }

    public void setInactive(Boolean inactive) {
        this.inactive = inactive;
    }

    public Boolean getTemporaryPassword() {
        return temporaryPassword;
    }

    public void setTemporaryPassword(Boolean temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }

    public String getLastAdmin() {
        return lastAdmin;
    }

    public void setLastAdmin(String lastAdmin) {
        this.lastAdmin = lastAdmin;
    }

    public AdminOptions getAdminOptions() {
        return adminOptions;
    }

    public void setAdminOptions(AdminOptions adminOptions) {
        this.adminOptions = adminOptions;
    }

    @Override
    public String toString() {
        return name + ", " + email + (BooleanUtils.isTrue(inactive) ? " (inactive)" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        UserSanitised that = (UserSanitised) o;

        return new EqualsBuilder()
                .append(name, that.name)
                .append(email, that.email)
                .append(inactive, that.inactive)
                .append(lastAdmin, that.lastAdmin)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .append(email)
                .append(inactive)
                .append(lastAdmin)
                .toHashCode();
    }
}
