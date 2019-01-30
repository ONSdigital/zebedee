package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.model.ServiceAccountWithToken;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.SessionsService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.util.function.Supplier;

import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;
import static com.github.onsdigital.zebedee.util.JsonUtils.writeResponse;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;

@Api
public class Service {

    public static final Error NOT_FOUND_ERR = new Error("not found");

    private Supplier<String> randomIdGenerator = () -> Random.id();
    private ServiceStore serviceStore;
    private SessionsService sessionsService;
    private PermissionsService permissionsService;
    private boolean datasetImportEnabled;

    /**
     * Construct a default Service API endpoint.
     */
    public Service() {
        this(cmsFeatureFlags().isEnableDatasetImport());
    }

    /**
     * Construct a Service API endpoint specifying if the dataset import feature is enabled.
     *
     * @param datasetImportEnabled
     */
    public Service(boolean datasetImportEnabled) {
        this.datasetImportEnabled = datasetImportEnabled;
    }

    @POST
    public void createService(HttpServletRequest request, HttpServletResponse response) throws IOException,
            NotFoundException, UnauthorizedException {
        // FIXME CMD feature.
        if (!datasetImportEnabled) {
            logWarn("Service endpoint is not supported as feature EnableDatasetImport is disabled")
                    .addParameter("responseStatus", SC_NOT_FOUND)
                    .log();
            writeResponse(response, NOT_FOUND_ERR, SC_NOT_FOUND);
            return;
        }

        logInfo("feature EnableDatasetImport is enabled").log();

        final Session session = getSessionsService().get(request);
        if (session != null && getPermissionsService().isAdministrator(session)) {
            final ServiceStore serviceStoreImpl = getServiceStore();
            final String token = randomIdGenerator.get();
            ServiceAccount service = serviceStoreImpl.store(token, request.getInputStream());
            logInfo("new service account created").addParameter("id", service.getId()).log();
            writeResponse(response, new ServiceAccountWithToken(service.getId(), token), SC_CREATED);
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
