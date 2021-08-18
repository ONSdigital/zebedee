package com.github.onsdigital.zebedee.keyring.migration;

import com.github.onsdigital.zebedee.keyring.KeyringException;

@FunctionalInterface
interface Rollback {

    void attempt() throws KeyringException;
}
