package com.github.onsdigital.zebedee.reader.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.onsdigital.zebedee.content.base.ContentLanguage;

@RunWith(MockitoJUnitRunner.class)
public class ReaderRequestUtilsTest {

    @Mock
    private HttpServletRequest request;

    @Test
    public void getRequestedLanguageEnglish() throws Exception {
        when(request.getParameter("lang")).thenReturn("en");
        assertThat(ReaderRequestUtils.getRequestedLanguage(request), equalTo(ContentLanguage.ENGLISH));
    }

    @Test
    public void getRequestedLanguageWelshh() throws Exception {
        when(request.getParameter("lang")).thenReturn("cy");
        assertThat(ReaderRequestUtils.getRequestedLanguage(request), equalTo(ContentLanguage.WELSH));
    }

    @Test
    public void getRequestedLanguageInvalid() throws Exception {
        when(request.getParameter("lang")).thenReturn("sv");
        assertThat(ReaderRequestUtils.getRequestedLanguage(request), is(nullValue()));
    }

    @Test
    public void getRequestedLanguageEmpty() throws Exception {
        when(request.getParameter("lang")).thenReturn("");
        assertThat(ReaderRequestUtils.getRequestedLanguage(request), is(nullValue()));
    }

    @Test
    public void getRequestedLanguageMissing() throws Exception {
        when(request.getParameter("lang")).thenReturn(null);
        assertThat(ReaderRequestUtils.getRequestedLanguage(request), is(nullValue()));
    }
}
