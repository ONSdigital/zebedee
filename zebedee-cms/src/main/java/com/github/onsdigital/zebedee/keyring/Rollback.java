package com.github.onsdigital.zebedee.keyring;

@FunctionalInterface
interface Rollback {

    void attempt() throws KeyringException;
}
