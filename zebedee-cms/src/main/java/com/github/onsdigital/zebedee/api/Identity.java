package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.authorisation.AuthorisationService;
import com.github.onsdigital.zebedee.authorisation.AuthorisationServiceImpl;
import com.github.onsdigital.zebedee.authorisation.UserIdentity;
import com.github.onsdigital.zebedee.authorisation.UserIdentityException;
import com.github.onsdigital.zebedee.json.JSONable;
import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import com.github.onsdigital.zebedee.service.ServiceStore;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

@Api
public class Identity {

	private AuthorisationService authorisationService;
    private ServiceStore serviceStore;

    static final String AUTHORIZATION_HEADER = "Authorization";

    @GET
	public void identifyUser(HttpServletRequest request, HttpServletResponse response) throws IOException,
			UserIdentityException {

		if (request.getHeader(AUTHORIZATION_HEADER) != null) {
			ServiceAccount serviceAccount = findService(request);
			if (serviceAccount != null) {
				if (request.getHeader(RequestUtils.TOKEN_HEADER) != null) {
					findUser(request, response);
					return;
				} else {
					writeResponse(response, new UserIdentity(serviceAccount.getId()), SC_OK);
				    return;
				}
			}
		}
		writeResponse(response, new Error("service not authenticated"), SC_UNAUTHORIZED);
	}

	private void findUser(HttpServletRequest request, HttpServletResponse response) throws IOException,
			UserIdentityException {
		String sessionID = RequestUtils.getSessionId(request);

		if (StringUtils.isEmpty(sessionID)) {
			Error responseBody = new Error("user not authenticated");
			logWarn(responseBody.getMessage()).log();
			writeResponse(response, responseBody, SC_UNAUTHORIZED);
			return;
		}

		try {
			UserIdentity identity = getAuthorisationService().identifyUser(sessionID);
			logInfo("authenticated user identity confirmed")
					.sessionID(sessionID)
					.user(identity.getIdentifier())
					.log();
			writeResponse(response, identity, SC_OK);
		} catch (UserIdentityException e) {
			logError(e, "identify user failure, returning error response").log();
			writeResponse(response, new Error(e.getMessage()), e.getResponseCode());
		}
	}

	private ServiceAccount findService(HttpServletRequest request) throws IOException {
        final ServiceStore serviceStore = getServiceStoreImpl();
		String authorizationHeader = request.getHeader("Authorization");
		if (authorizationHeader != null) {
			authorizationHeader = authorizationHeader.toLowerCase();
			if (authorizationHeader.toLowerCase().startsWith("bearer ")) {
				String[] cred = authorizationHeader.split("bearer ");
				String token = cred[1];
				final ServiceAccount service = serviceStore.get(token);
				if (service != null) {
					logInfo("authenticated service account confirmed")
							.user(service.getId())
							.log();
					return service;
				} else {
					return null;
				}
			}
		}
		return null;
	}

	private void writeResponse(HttpServletResponse response, JSONable body, int status) throws IOException {
		try {
			response.setStatus(status);
			response.setContentType(APPLICATION_JSON);
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.getWriter().write(body.toJSON());
		} catch (IOException e) {
			logError(e, "error while attempting to write userIdentity to HTTP response").log();
			throw e;
		}
	}

	private AuthorisationService getAuthorisationService() {
		if (authorisationService == null) {
			this.authorisationService = new AuthorisationServiceImpl();
		}
		return authorisationService;
	}

	private ServiceStore getServiceStoreImpl() {
        if (serviceStore == null) {
            this.serviceStore = Root.zebedee.getServiceStore();
        }
        return serviceStore;
    }

}
