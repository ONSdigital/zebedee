package com.github.onsdigital.zebedee.api;


import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Configuration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;

public class CollectionIT {

    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").create();

    private String authenticationToken = "";

    public static CollectionDescription createCollection(String authenticationToken) {
        CollectionDescription description = new CollectionDescription();
        description.name = UUID.randomUUID().toString();
        description.publishDate = new Date();

        Response postResponse = given()
                .header(LoginIT.tokenHeader, authenticationToken)
                .body(gson.toJson(description))
                .post(Configuration.getBaseUrl() + "/collection");
        postResponse.then().assertThat().statusCode(200);

        return description;
    }

    @Before
    public void setUp() throws Exception {
        authenticationToken = LoginIT.login();
    }

    @Test
    public void shouldCreateCollection() {

        CollectionDescription description = createCollection(authenticationToken);

        Response postResponse = given()
                .header("X-Florence-Token", authenticationToken)
                .body(gson.toJson(description))
                .post(Configuration.getBaseUrl() + "/collection");
        postResponse.then().assertThat().statusCode(409);
    }

    @Test
    public void shouldUpdateCollection() {

        CollectionDescription description = createCollection(authenticationToken);

        String oldName = description.name;
        Date oldPublishDate = description.publishDate;

        description.name = UUID.randomUUID().toString();
        description.publishDate = new Date();

        // Update the collection with the new
        Response postResponse = given().body(gson.toJson(description)).post(Configuration.getBaseUrl() + "/collection/" + oldName);
        postResponse.then().assertThat().statusCode(200);

        Response response = get(Configuration.getBaseUrl() + "/collection/" + description.name);
        response.then().assertThat().statusCode(200);
    }

    @Test
    public void shouldReturn409IfUpdateNameAlreadyExists() {

        CollectionDescription description = createCollection(authenticationToken);
        String existingName = description.name;

        description = createCollection(authenticationToken);

        Response postResponse = given().body(gson.toJson(description)).post(Configuration.getBaseUrl() + "/collection/" + existingName);
        postResponse.then().assertThat().statusCode(409);
    }

    @Test
    public void shouldGetCollection() {

        CollectionDescription description = createCollection(authenticationToken);

        Response response = get(Configuration.getBaseUrl() + "/collection/" + description.name);
        response.then().assertThat().statusCode(200);
    }
}
