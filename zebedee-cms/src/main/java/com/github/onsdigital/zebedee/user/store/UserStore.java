package com.github.onsdigital.zebedee.user.store;

import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;

import java.io.IOException;

/**
 * Created by dave on 30/05/2017.
 */
public interface UserStore {

    /**
     *
     * @param email
     * @return
     * @throws IOException
     */
    boolean exists(String email) throws IOException;

    /**
     *
     * @param email
     * @return
     * @throws IOException
     */
    User get(String email) throws IOException;

    /**
     *
     * @param user
     * @throws IOException
     */
    void save(User user) throws IOException;

    /**
     *
     * @return
     * @throws IOException
     */
    UserList list() throws IOException;


    /**
     *
     * @param session
     * @param user
     * @return
     * @throws IOException
     * @throws UnauthorizedException
     * @throws NotFoundException
     */
    boolean delete(User user) throws IOException, UnauthorizedException, NotFoundException;
}
