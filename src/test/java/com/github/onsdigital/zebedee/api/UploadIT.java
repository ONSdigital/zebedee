package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.configuration.Configuration;
import com.jayway.restassured.builder.MultiPartSpecBuilder;
import com.jayway.restassured.specification.MultiPartSpecification;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

import static org.junit.Assert.*;

public class UploadIT {

    private String authenticationToken = "";

    @Before
    public void setUp() throws Exception {
        authenticationToken = LoginIT.login();
    }


    @Test
    public void testUpload() throws Exception {
        // Given
        // ... a file to upload to a destination
        String TAXONOMY = "/alpha/beta";
        String FILE_NAME = "/uploadme.txt";

        // When
        // ... we upload the file

        // We expect
        // ... the file to be present

        assertEquals(true, true);
    }

    @Test
    public void testUploadAndReplace() throws Exception {
        // Given
        // ... a file we upload to a destination
        // ... a file to replace it

        // When
        // ... we upload the file

        // We expect
        // ... the file to be present
    }
}