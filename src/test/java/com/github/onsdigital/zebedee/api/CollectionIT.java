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

public class CollectionIT {

    public static CollectionDescription CreateCollection() {
        CollectionDescription description = new CollectionDescription();
        description.name = UUID.randomUUID().toString();
        description.publishDate = new SimpleDateFormat().format(new Date());

        Response postResponse = given().body(description).post(Configuration.getBaseUrl() + "/collection");
        postResponse.then().assertThat().statusCode(200);

        return description;
    }

    @Test
    public void shouldCreateCollection() {

        CollectionDescription description = CreateCollection();

        Response postResponse = given().body(description).post(Configuration.getBaseUrl() + "/collection");
        postResponse.then().assertThat().statusCode(409);
    }

    @Test
    public void shouldUpdateCollection() {

        CollectionDescription description = CreateCollection();

        String oldName = description.name;
        String oldPublishDate = description.publishDate;

        description.name = UUID.randomUUID().toString();
        description.publishDate = new SimpleDateFormat().format(new Date());

        // Update the collection with the new
        Response postResponse = given().body(description).post(Configuration.getBaseUrl() + "/collection/" + oldName);
        postResponse.then().assertThat().statusCode(200);

        Response response = get(Configuration.getBaseUrl() + "/collection/" + description.name);
        response.then().assertThat().statusCode(200);
    }

    @Test
    public void shouldReturn409IfUpdateNameAlreadyExists() {

        CollectionDescription description = CreateCollection();
        String existingName = description.name;

        description = CreateCollection();

        Response postResponse = given().body(description).post(Configuration.getBaseUrl() + "/collection/" + existingName);
        postResponse.then().assertThat().statusCode(409);
    }

    @Test
    public void shouldGetCollection() {

        CollectionDescription description = CreateCollection();

        Response response = get(Configuration.getBaseUrl() + "/collection/" + description.name);
        response.then().assertThat().statusCode(200);
    }
}
