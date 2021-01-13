package com.github.onsdigital.zebedee.keyring;

import java.security.KeyException;

public interface KeyringStore {

    void write(Keyring keyring) throws KeyException;
}
