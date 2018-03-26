package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.authorisation.UserIdentity;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.JSONable;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.model.ServiceAccountWithToken;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.service.ServiceStoreImpl;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import org.apache.commons.lang3.BooleanUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.util.function.Supplier;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.util.JsonUtils.writeResponse;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;

@Api
public class Service {

    private Supplier<String> randomIdGenerator = () -> Random.id();

    private ServiceStore serviceStore;

    private SessionsService sessionsService;

    private PermissionsService permissionsService;

    @POST
    public void createService(HttpServletRequest request, HttpServletResponse response) throws IOException, NotFoundException, UnauthorizedException {
        final Session session = getSessionsService().get(request);
        if (session != null && getPermissionsService().isAdministrator(session)) {
            final ServiceStore serviceStoreImpl = getServiceStore();
            final String token = randomIdGenerator.get();
            ServiceAccount service = serviceStoreImpl.store(token, request.getInputStream());
            logInfo("new service account created").addParameter("id", service.getId()).log();
            writeResponse(response, new ServiceAccountWithToken(token, service.getId()), SC_CREATED);
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

    private SessionsService getSessionsService() {
        if (sessionsService == null) {
            sessionsService = Root.zebedee.getSessionsService();
        }
        return sessionsService;
    }

    private PermissionsService getPermissionsService() {
        if (permissionsService == null) {
            permissionsService = Root.zebedee.getPermissionsService();
        }
        return permissionsService;
    }
}
