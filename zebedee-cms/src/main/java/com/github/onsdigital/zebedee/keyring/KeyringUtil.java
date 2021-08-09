package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;

import java.io.IOException;

import static java.text.MessageFormat.format;

public class KeyringUtil {

    public static final String GET_USER_ERR_FMT = "get user returned unexpected error: {0}";
    public static final String USER_NOT_FOUND_ERR_FMT = "requested user was not found: {0}";

    private KeyringUtil() {
        // Utility class with static only methods so hide constructor.
    }

    /**
     * Get a {@link User} by email.
     *
     * @param usersService the {@link UsersService} to use.
     * @param email        the email address of the user to get.
     * @return the user if found. Throws {@link NotFoundException} if user not found or is null.
     * @throws IOException         error getting user.
     * @throws InternalServerError unexpected error getting user.
     * @throws NotFoundException   thrown if user not found.
     * @throws BadRequestException thrown of the user email is invalid.
     */
    public static User getUser(UsersService usersService, String email) throws InternalServerError,
            NotFoundException, BadRequestException {
        if (usersService == null) {
            throw new InternalServerError("get user requires non null userService");
        }

        User user;
        try {
            user = usersService.getUserByEmail(email);
        } catch (IOException ex) {
            String message = format(GET_USER_ERR_FMT, email);
            throw new InternalServerError(message, ex);
        }

        if (user == null) {
            throw new NotFoundException(format(USER_NOT_FOUND_ERR_FMT, email));
        }

        return user;
    }
}
