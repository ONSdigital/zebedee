package com.github.onsdigital.zebedee.api;


import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.jayway.restassured.response.Response;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;

public class CollectionIT {

    private static final String baseUrl = "http://localhost:8082";

    @Test
    public void shouldCreateCollection() {

        CollectionDescription desc = new CollectionDescription();
        desc.name = UUID.randomUUID().toString(); // "Inflation Q2 2015";
        desc.publishDate = new SimpleDateFormat().format(new Date());

        Response postResponse = given().body(desc).post(baseUrl + "/collection");
        postResponse.then().assertThat().statusCode(200);
    }


    @Test
    public void shouldGetCollection() {

        CollectionDescription desc = new CollectionDescription();
        desc.name = UUID.randomUUID().toString(); // "Inflation Q2 2015";
        desc.publishDate = new SimpleDateFormat().format(new Date());

        Response postResponse = given().body(desc).post(baseUrl + "/collection");
        postResponse.then().assertThat().statusCode(200);

        Response response = get(baseUrl + "/collection/" + desc.name);
        response.then().assertThat().statusCode(200);
    }
}
