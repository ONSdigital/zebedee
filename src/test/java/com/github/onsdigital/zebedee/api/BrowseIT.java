package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Configuration;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.jayway.restassured.response.Response;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

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
        CollectionDescription desc = new CollectionDescription();
        desc.name = UUID.randomUUID().toString();
        desc.publishDate = new SimpleDateFormat().format(new Date());

        given().body(desc).post(Configuration.getBaseUrl() + "/collection");

        Response response = given()
                .get(Configuration.getBaseUrl() + "/browse/" + desc.name);
        response.then().assertThat().statusCode(200);
    }
}
