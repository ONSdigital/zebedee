package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Configuration;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jayway.restassured.response.Response;
import org.junit.Test;

import java.util.List;

import static com.jayway.restassured.RestAssured.get;
import static org.junit.Assert.fail;

public class CollectionsIT {

    @Test
    public void shouldGetCollections() {

        CollectionDescription description = CollectionIT.CreateCollection();

        Response getResponse = get(Configuration.getBaseUrl() + "/collections");
        getResponse.then().assertThat().statusCode(200);

        List<CollectionDescription> collections = new
                Gson().fromJson(getResponse.asString(),
                new TypeToken<List<CollectionDescription>>() {
                }.getType());


        for (CollectionDescription collection : collections) {
            if (collection.name.equals(description.name))
                return;
        }

        fail("The collection was not found");
    }
}
