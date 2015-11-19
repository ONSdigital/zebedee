package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by thomasridd on 18/11/15.
 */
public class KeyManagerTest {
    Zebedee zebedee;
    Builder builder;

    @Before
    public void setUp() throws Exception {
        builder = new Builder(this.getClass());
        zebedee = new Zebedee(builder.zebedee);
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }

    @Test
    public void isEncrypted_whenCollectionGenerated_isSetToFalse() {
        // Given
        // a collection is created

        // When
        // we reload it

        // Then
        // isEncrypted is false

    }

    @Test
    public void isEncrypted_whenSetToTrue_persists() {
        // Given
        // a collection is created, isEncrypted is set, and is set to true

        // When
        // we reload the collection

        // Then
        // isEncrypted is true

    }

    @Test
    public void userKeyring_whenCollectionGenerated_hasKeyForCollection() {
        // Given
        // a collection is created

        // When
        // we look at the user keyring

        // Then
        // it has a key for the collection

    }

    @Test
    public void publisherKeyring_whenCollectionGenerated_hasKeyForCollection() {
        // Given
        // a collection is created by publisher A

        // When
        // we look at publisher B's keyring

        // Then
        // it has a key for the collection

    }
}