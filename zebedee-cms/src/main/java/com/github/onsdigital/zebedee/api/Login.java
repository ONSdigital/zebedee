package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * API for processing login requests.
 */
@Api
public class Login {

    /**
     * Wrap static method calls to obtain service in function makes testing easier - class member can be
     * replaced with a mocked giving control of desired behaviour.
     */
    private ServiceSupplier<UsersService> usersServiceSupplier = () -> Root.zebedee.getUsersService();

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
     * @return A session ID to be passed in the {@value SessionsService#TOKEN_HEADER} header.
     * @throws IOException
     */
    @POST
    public String authenticate(HttpServletRequest request, HttpServletResponse response, Credentials credentials) throws IOException, NotFoundException, BadRequestException {

        if (credentials == null || StringUtils.isBlank(credentials.getEmail())) {
            logInfo("login request unsuccessful - no credentials provided").log();
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return "Please provide credentials (email, password).";
        }

        User user = usersServiceSupplier.getService().getUserByEmail(credentials.getEmail());
        boolean result = user.authenticate(credentials.password);

        if (!result) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            Audit.Event.LOGIN_AUTHENTICATION_FAILURE.parameters().host(request).user(credentials.getEmail()).log();
            logInfo("login request unsuccessful - credentials were not authenticated successfully").user(user.getEmail()).log();
            return "Authentication failed.";
        }

        // Temponary whilst encryption is being put in place.
        // This can be removed once all users have keyrings.
        usersServiceSupplier.getService().migrateToEncryption(user, credentials.getPassword());
        usersServiceSupplier.getService().removeStaleCollectionKeys(user.getEmail());

        if (BooleanUtils.isTrue(user.getTemporaryPassword())) {
            logInfo("login request unsuccessful - password change is required").user(user.getEmail()).log();
            response.setStatus(HttpStatus.EXPECTATION_FAILED_417);
            Audit.Event.LOGIN_PASSWORD_CHANGE_REQUIRED.parameters().host(request).user(credentials.getEmail()).log();
            return "Password change required";
        } else {
            Audit.Event.LOGIN_SUCCESS.parameters().host(request).user(credentials.getEmail()).log();
            logInfo("login request successful").user(credentials.getEmail()).log();
            response.setStatus(HttpStatus.OK_200);
        }

        return Root.zebedee.openSession(credentials).getId();
    }

}
