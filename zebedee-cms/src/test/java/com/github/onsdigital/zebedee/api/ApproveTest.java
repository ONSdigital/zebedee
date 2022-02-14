package com.github.onsdigital.zebedee.api;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;

import static com.github.onsdigital.zebedee.api.Approve.OVERRIDE_KEY_PARAM;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

public class ApproveTest {


    @Mock
    private HttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getOverrideKey_paramNull_shouldReturnNull() {
        when(request.getParameter(OVERRIDE_KEY_PARAM))
                .thenReturn(null);

        Long key = new Approve().getOverrideKey(request);
        assertThat(key, is(nullValue()));
    }

    @Test
    public void getOverrideKey_nonNumericValue_shouldReturnNull() {
        when(request.getParameter(OVERRIDE_KEY_PARAM))
                .thenReturn("abc");

        Long key = new Approve().getOverrideKey(request);
        assertThat(key, is(nullValue()));
    }

    @Test
    public void getOverrideKey_numericValue_shouldReturnValueAsLong() {
        when(request.getParameter(OVERRIDE_KEY_PARAM))
                .thenReturn("666");

        Long key = new Approve().getOverrideKey(request);
        assertThat(key, equalTo(666L));
    }
}
