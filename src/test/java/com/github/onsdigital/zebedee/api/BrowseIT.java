package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Configuration;
import com.jayway.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;

public class BrowseIT {

    private String authenticationToken = "";

    @Before
    public void setUp() throws Exception {
        authenticationToken = LoginIT.login();
    }

    @Test
    public void shouldReturn404IfNoPathSpecified() {
        Response response = get(Configuration.getBaseUrl() + "/browse");
        response.then().assertThat().statusCode(404);
    }

    @Test
    public void shouldReturn200WithValidCollectionName() {

        CollectionDescription description = CollectionIT.createCollection(authenticationToken);

        Response response = given()
                .get(Configuration.getBaseUrl() + "/browse/" + description.name);
        response.then().assertThat().statusCode(200);
    }
}
