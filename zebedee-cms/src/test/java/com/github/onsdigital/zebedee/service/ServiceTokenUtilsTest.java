package com.github.onsdigital.zebedee.service;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.onsdigital.zebedee.service.ServiceTokenUtils.BEARER_PREFIX_UC;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServiceTokenUtilsTest {

    @Test
    public void testIsValidServiceToken_null() {
        assertFalse(ServiceTokenUtils.isValidServiceToken(null));
    }

    @Test
    public void testIsValidServiceToken_empty() {
        assertFalse(ServiceTokenUtils.isValidServiceToken(""));
    }

    @Test
    public void testIsValidServiceToken_lessThatMinLength() {
        assertFalse(ServiceTokenUtils.isValidServiceToken("123dfg54d7"));
    }

    @Test
    public void testIsValidServiceToken_containsPeriod() {
        assertFalse(ServiceTokenUtils.isValidServiceToken("d8b90a24c3d247aeaf84731e4e69dd6f."));
    }

    @Test
    public void testIsValidServiceToken_containsSlash() {
        assertFalse(ServiceTokenUtils.isValidServiceToken("/d8b90a24c3d247aeaf84731e4e69dd6f"));
    }

    @Test
    public void testIsValidServiceToken_containsPeriodAndSlash() {
        assertFalse(ServiceTokenUtils.isValidServiceToken("./d8b90a24c3d247aeaf84731e4e69dd6f"));
    }

    @Test
    public void testIsValidServiceToken_containsInvalidCharacters() {
        assertFalse(ServiceTokenUtils.isValidServiceToken("./d8b90a24c3-d247aeaf8_+()4731e4e69dd6f"));
    }

    @Test
    public void testIsValidServiceToken_success() {
        assertTrue(ServiceTokenUtils.isValidServiceToken("d8b90a24c3d247aeaf84731e4e69dd6f"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getServiceTokenPath_ShouldThrowExIfPathNull() {
        try {
            ServiceTokenUtils.getServiceAccountPath(null, null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("service dir path required but was empty or null"));
            throw ex;
        }
    }

    @Test
    public void getServiceTokenPath_Success() {
        Path servicePath = Paths.get("/service");
        String token = "d8b90a24c3d247aeaf84731e4e69dd6f";

        Path actual = ServiceTokenUtils.getServiceAccountPath(servicePath, token);
        assertThat(actual, equalTo(servicePath.resolve(token + ".json")));
    }

    @Test
    public void testGetTokenFilename() {
        String token = "d8b90a24c3d247aeaf84731e4e69dd6f";
        assertThat(ServiceTokenUtils.getTokenFilename(token), equalTo(token + ".json"));
    }

    @Test
    public void testIsValidServiceAuthorizationHeader_null() {
        assertFalse(ServiceTokenUtils.isValidServiceAuthorizationHeader(null));
    }

    @Test
    public void testIsValidServiceAuthorizationHeader_empty() {
        assertFalse(ServiceTokenUtils.isValidServiceAuthorizationHeader(""));
    }

    @Test
    public void testIsValidServiceAuthorizationHeader_noPrefix() {
        assertFalse(ServiceTokenUtils.isValidServiceAuthorizationHeader("d8b90a24c3d247aeaf84731e4e69dd6f"));
    }

    @Test
    public void testIsValidServiceAuthorizationHeader_lowercasePrefix() {
        assertFalse(ServiceTokenUtils.isValidServiceAuthorizationHeader("bearer d8b90a24c3d247aeaf84731e4e69dd6f"));
    }

    @Test
    public void testIsValidServiceAuthorizationHeader_success() {
        assertTrue(ServiceTokenUtils.isValidServiceAuthorizationHeader("Bearer d8b90a24c3d247aeaf84731e4e69dd6f"));
    }

    @Test
    public void testExtractServiceAccountTokenFromAuthHeader_null() {
        String actual = ServiceTokenUtils.extractServiceAccountTokenFromAuthHeader(null);
        assertThat(actual, is(nullValue()));
    }

    @Test
    public void testExtractServiceAccountTokenFromAuthHeader_empty() {
        String actual = ServiceTokenUtils.extractServiceAccountTokenFromAuthHeader("");
        assertThat(actual, equalTo(""));
    }

    @Test
    public void testExtractServiceAccountTokenFromAuthHeader_invalidPrefix() {
        String input = "bearer d8b90a24c3d247aeaf84731e4e69dd6f";
        String actual = ServiceTokenUtils.extractServiceAccountTokenFromAuthHeader(input);
        assertThat(actual, equalTo(input));
    }

    @Test
    public void testExtractServiceAccountTokenFromAuthHeader_noPrefix() {
        String input = "d8b90a24c3d247aeaf84731e4e69dd6f";
        String actual = ServiceTokenUtils.extractServiceAccountTokenFromAuthHeader(input);
        assertThat(actual, equalTo(input));
    }

    @Test
    public void testExtractServiceAccountTokenFromAuthHeader_validInput() {
        String token = "d8b90a24c3d247aeaf84731e4e69dd6f";
        String input = BEARER_PREFIX_UC + " " + token;

        String actual = ServiceTokenUtils.extractServiceAccountTokenFromAuthHeader(input);
        assertThat(actual, equalTo(token));
    }
}
