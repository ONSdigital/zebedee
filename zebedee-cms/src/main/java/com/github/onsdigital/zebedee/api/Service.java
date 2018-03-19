package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.model.ServiceAccount;
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

@Api
public class Service {

    private Supplier<String> randomIdGenerator = () -> Random.id();

    private ServiceStore serviceStore;

    private SessionsService sessionsService;

    private PermissionsService permissionsService;

    @POST
    public ServiceAccountWithToken createService(HttpServletRequest request, HttpServletResponse response) throws IOException, NotFoundException, UnauthorizedException {
        final Session session = getSessionsService().get(request);
        PermissionDefinition permissionDefinition = getPermissionsService().userPermissions(session.getEmail(), session);
        if (BooleanUtils.isTrue(permissionDefinition.admin)) {
            final ServiceStore serviceStoreImpl = getServiceStore();
            final String token = randomIdGenerator.get();
            ServiceAccount service = serviceStoreImpl.store(token, request.getInputStream());
            logInfo("new service account created").addParameter("id", service.getId()).log();
            response.setStatus(HttpServletResponse.SC_CREATED);
            return new ServiceAccountWithToken(token, service.getId());
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return null;
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

    private class ServiceAccountWithToken  extends ServiceAccount{

        private final String token;

        ServiceAccountWithToken(String id, String token) {
            super(id);
            this.token = token;
        }

        public String getToken() {
            return token;
        }
    }
}
