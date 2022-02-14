package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.model.ServiceAccountWithToken;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.util.function.Supplier;

import static com.github.onsdigital.zebedee.util.JsonUtils.writeResponseEntity;
import static org.apache.http.HttpStatus.SC_CREATED;

/**
 * @deprecated The POST /service endpoint is deprecated in favour of the dp-identity-api. Once the migration of all
 *             automated service users to the dp-identity-api, this endpoint will be removed.
 */
@Deprecated
@Api
public class Service {

    public static final Error NOT_FOUND_ERR = new Error("not found");

    private Supplier<String> randomIdGenerator = Random::id;
    private ServiceStore serviceStore;
    private Sessions sessions;
    private PermissionsService permissionsService;

    @POST
    public void createService(HttpServletRequest request, HttpServletResponse response) throws IOException {

        final Session session = getSessions().get();
        if (session != null && getPermissionsService().isAdministrator(session)) {
            final ServiceStore serviceStoreImpl = getServiceStore();
            final String token = randomIdGenerator.get();
            ServiceAccount service = serviceStoreImpl.store(token, request.getInputStream());
            writeResponseEntity(response, new ServiceAccountWithToken(service.getID(), token), SC_CREATED);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    private ServiceStore getServiceStore() {
        if (serviceStore == null) {
            serviceStore = Root.zebedee.getServiceStore();
        }
        return serviceStore;
    }

    private Sessions getSessions() {
        if (sessions == null) {
            sessions = Root.zebedee.getSessions();
        }
        return sessions;
    }

    private PermissionsService getPermissionsService() {
        if (permissionsService == null) {
            permissionsService = Root.zebedee.getPermissionsService();
        }
        return permissionsService;
    }
}
