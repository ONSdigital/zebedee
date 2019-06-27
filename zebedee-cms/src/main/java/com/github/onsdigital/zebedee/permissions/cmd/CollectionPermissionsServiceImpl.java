package com.github.onsdigital.zebedee.permissions.cmd;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;

import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;

public class CollectionPermissionsServiceImpl implements CollectionPermissionsService {

    private PermissionsService permissionsService;

    public CollectionPermissionsServiceImpl(PermissionsService permissionsService) {
        this.permissionsService = permissionsService;
    }

    @Override
    public boolean hasEdit(Session session) throws PermissionsException {
        try {
            return permissionsService.canEdit(session);
        } catch (IOException ex) {
            error().exception(ex).user(session).data("permission", "can_edit")
                    .log("user dataset permissions request denied error checking user permissions");
            throw new PermissionsException("internal server error", SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean hasView(Session session, CollectionDescription description)
            throws PermissionsException {
        try {
            return permissionsService.canView(session, description);
        } catch (IOException ex) {
            error().exception(ex).user(session).data("permission", "can_view")
                    .log("user dataset permissions request denied error checking user permissions");
            throw new PermissionsException("internal server error", SC_INTERNAL_SERVER_ERROR);
        }
    }
}
