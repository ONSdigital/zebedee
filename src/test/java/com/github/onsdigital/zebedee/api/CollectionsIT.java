package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Configuration;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jayway.restassured.response.Response;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.fail;

public class CollectionsIT {

    @Test
    public void shouldGetCollections() {
        CollectionDescription desc = new CollectionDescription();
        desc.name = UUID.randomUUID().toString();
        desc.publishDate = new SimpleDateFormat().format(new Date());

        Response postResponse = given().body(desc).post(Configuration.getBaseUrl() + "/collection");
        postResponse.then().assertThat().statusCode(200);

        Response getResponse = get(Configuration.getBaseUrl() + "/collections");
        postResponse.then().assertThat().statusCode(200);

        List<CollectionDescription> collections = new
                Gson().fromJson(getResponse.asString(),
                new TypeToken<List<CollectionDescription>>() {
                }.getType());


        for (CollectionDescription collection : collections) {
            if (collection.name.equals(desc.name))
                return;
        }

        fail("The collection was not found");
    }
}
