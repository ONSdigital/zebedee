package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Configuration;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.jayway.restassured.response.Response;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;

public class BrowseIT {
    @Test
    public void shouldReturn404IfNoPathSpecified() {
        Response response = get(Configuration.getBaseUrl() + "/browse");
        response.then().assertThat().statusCode(404);
    }

    @Test
    public void shouldReturn200WithValidCollectionName() {

        CollectionDescription description = CollectionIT.CreateCollection();

        Response response = given()
                .get(Configuration.getBaseUrl() + "/browse/" + description.name);
        response.then().assertThat().statusCode(200);
    }
}
