package com.github.onsdigital.zebedee.json;

import com.github.davidcarboni.cryptolite.Password;

/**
 * Represents a user account. NB this record intentionally does not contain any permission-related information.
 * This is purely acconut information.
 * Created by david on 12/03/2015.
 */
public class User {

    public String name;
    public String email;
    public String passwordHash;

    /**
     * This field is {@link Boolean} rather than <code>boolean</code> so that it can be <code>null</code> in an update message.
     * This ensures the value won't change unless explicitly specified.
     */
    public Boolean inactive;

    public static void main(String[] args) {
        String password = Password.hash("password");
        System.out.println("password = " + password);
    }
}
