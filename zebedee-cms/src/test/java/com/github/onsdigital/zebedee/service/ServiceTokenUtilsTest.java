package com.github.onsdigital.zebedee.service;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.onsdigital.zebedee.service.ServiceTokenUtils.EMPTY_TOKEN_ERROR;
import static com.github.onsdigital.zebedee.service.ServiceTokenUtils.INVALID_TOKEN_ERROR;
import static com.github.onsdigital.zebedee.service.ServiceTokenUtils.MIN_TOKEN_LENGTH;
import static com.github.onsdigital.zebedee.service.ServiceTokenUtils.SERVICE_PATH_EMPTY_ERROR;
import static com.github.onsdigital.zebedee.service.ServiceTokenUtils.TOKEN_LENGTH_INVALID_ERROR;
import static java.text.MessageFormat.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ServiceTokenUtilsTest {

    @Test(expected = RuntimeException.class)
    public void validateTokenShouldRejectNullToken() {
        try {
            ServiceTokenUtils.validateToken(null);
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), equalTo(EMPTY_TOKEN_ERROR));
            throw ex;
        }
    }

    @Test(expected = RuntimeException.class)
    public void validateTokenShouldRejectEmptyToken() {
        try {
            ServiceTokenUtils.validateToken("");
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), equalTo(EMPTY_TOKEN_ERROR));
            throw ex;
        }
    }

    @Test(expected = RuntimeException.class)
    public void validateTokenShouldRejectTokenWithSpace() {
        try {
            ServiceTokenUtils.validateToken("123 abc");
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_TOKEN_ERROR));
            throw ex;
        }
    }

    @Test(expected = RuntimeException.class)
    public void validateTokenShouldRejectTokenWithInvalidCharacter() {
        try {
            ServiceTokenUtils.validateToken("123_abc");
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_TOKEN_ERROR));
            throw ex;
        }
    }

    @Test(expected = RuntimeException.class)
    public void validateTokenShouldRejectTokenWithPeriod() {
        try {
            ServiceTokenUtils.validateToken(".123_abc");
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_TOKEN_ERROR));
            throw ex;
        }
    }

    @Test(expected = RuntimeException.class)
    public void validateTokenShouldRejectTokenWithSlash() {
        try {
            ServiceTokenUtils.validateToken("/123_abc");
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_TOKEN_ERROR));
            throw ex;
        }
    }

    @Test(expected = RuntimeException.class)
    public void validateTokenShouldRejectTokenWithPeriodAndSlash() {
        try {
            ServiceTokenUtils.validateToken("../123_abc");
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_TOKEN_ERROR));
            throw ex;
        }
    }

    @Test(expected = RuntimeException.class)
    public void validateTokenShouldRejectTokenShorterThanMinLength() {
        try {
            ServiceTokenUtils.validateToken("1wed5438u");
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), equalTo(format(TOKEN_LENGTH_INVALID_ERROR, MIN_TOKEN_LENGTH)));
            throw ex;
        }
    }

    @Test
    public void validateTokenShouldAllowTokenWithOnlyNumerics() {
        ServiceTokenUtils.validateToken("1234567890123456");
    }

    @Test
    public void validateTokenShouldAllowValidToken() {
        ServiceTokenUtils.validateToken("d8b90a24c3d247aeaf84731e4e69dd6f");
    }

    @Test(expected = RuntimeException.class)
    public void getServiceTokenPath_ShouldThrowExIfPathNull() {
        try {
            ServiceTokenUtils.getServiceTokenPath(null, null);
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), equalTo(SERVICE_PATH_EMPTY_ERROR));
            throw ex;
        }
    }

    @Test(expected = RuntimeException.class)
    public void getServiceTokenPath_ShouldThrowExIfTokenNull() {
        try {
            ServiceTokenUtils.getServiceTokenPath(Paths.get("/test"), null);
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), equalTo(EMPTY_TOKEN_ERROR));
            throw ex;
        }
    }

    @Test(expected = RuntimeException.class)
    public void getServiceTokenPath_ShouldThrowExIfTokenInvalid() {
        try {
            ServiceTokenUtils.getServiceTokenPath(Paths.get("/test"), "__^&*()_");
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_TOKEN_ERROR));
            throw ex;
        }
    }

    @Test
    public void getServiceTokenPath_Success() {
        Path servicePath = Paths.get("/service");
        String token = "d8b90a24c3d247aeaf84731e4e69dd6f";

        Path actual = ServiceTokenUtils.getServiceTokenPath(servicePath, token);
        assertThat(actual, equalTo(servicePath.resolve(token + ".json")));
    }

    @Test
    public void testGetTokenFilename() {
        String token = "d8b90a24c3d247aeaf84731e4e69dd6f";
        assertThat(ServiceTokenUtils.getTokenFilename(token), equalTo(token + ".json"));
    }
}
