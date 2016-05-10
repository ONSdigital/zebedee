package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

/**
 * API for processing login requests.
 */
@Api
public class Login {

    private static final String LOGIN_SUCCESS_MSG = "Florence login success";
    private static final String LOGIN_AUTH_FAILURE_MSG = "Login authentication failure";
    private static final String PASSWORD_CHANGE_REQUIRED_MSG = "Florence password change required";

    /**
     * Authenticates with Zebedee.
     *
     * @param request     This should contain a {@link Credentials} Json object.
     * @param response    <ul>
     *                    <li>If authentication succeeds: a new or existing session ID.</li>
     *                    <li>If credentials are not provided:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                    <li>If authentication fails:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                    </ul>
     * @param credentials The user email and password.
     * @return A session ID to be passed in the {@value com.github.onsdigital.zebedee.model.Sessions#TOKEN_HEADER} header.
     * @throws IOException
     */
    @POST
    public String authenticate(HttpServletRequest request, HttpServletResponse response, Credentials credentials) throws IOException, NotFoundException, BadRequestException {

        if (credentials == null || StringUtils.isBlank(credentials.email)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return "Please provide credentials (email, password).";
        }

        User user = Root.zebedee.users.get(credentials.email);
        boolean result = user.authenticate(credentials.password);

        if (!result) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            Audit.Event.LOGIN_AUTHENTICATION_FAILURE.parameters().host(request).user(credentials.email).log();
            ZebedeeLogBuilder.logInfo(LOGIN_AUTH_FAILURE_MSG).user(credentials.email).log();
            return "Authentication failed.";
        }

        // Temponary whilst encryption is being put in place.
        // This can be removed once all users have keyrings.
        com.github.onsdigital.zebedee.model.Users.migrateToEncryption(Root.zebedee, user, credentials.password);
        com.github.onsdigital.zebedee.model.Users.cleanupCollectionKeys(Root.zebedee, user);

        if (BooleanUtils.isTrue(user.temporaryPassword)) {
            response.setStatus(HttpStatus.EXPECTATION_FAILED_417);
            Audit.Event.LOGIN_PASSWORD_CHANGE_REQUIRED.parameters().host(request).user(credentials.email).log();
            ZebedeeLogBuilder.logInfo(PASSWORD_CHANGE_REQUIRED_MSG).user(credentials.email).log();
            return "Password change required";
        } else {
            Audit.Event.LOGIN_SUCCESS.parameters().host(request).user(credentials.email).log();
            ZebedeeLogBuilder.logInfo(LOGIN_SUCCESS_MSG).user(credentials.email).log();
            response.setStatus(HttpStatus.OK_200);
        }

        return Root.zebedee.openSession(credentials).id;
    }

}
