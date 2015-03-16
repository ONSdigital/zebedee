package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Configuration;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.jayway.restassured.response.Response;
import org.junit.Test;

import java.util.UUID;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

public class ContentIT {

    @Test
    public void shouldReturn400WhenNoUriIsSpecified() {
        CollectionDescription description = CollectionIT.createCollection();

        Response getResponse = get(Configuration.getBaseUrl() + "/content/" + description.name);
        getResponse.then().assertThat().statusCode(400);
    }

    @Test
    public void shouldAddContent() {
        CollectionDescription description = CollectionIT.createCollection();

        String content = "this is content";
        String directory = UUID.randomUUID().toString();
        String path = directory + "/data.json";

        Response postResponse = given().body(content).post(
                Configuration.getBaseUrl() + "/content/" + description.name + "?uri=" + path);
        postResponse.then().assertThat().statusCode(200);

        Response getResponse = get(Configuration.getBaseUrl() + "/content/" + description.name + "?uri=" + path);
        getResponse.then().assertThat().statusCode(200);

        assertEquals(content, getResponse.asString());
    }

    @Test
    public void shouldUpdateContent() {
        CollectionDescription description = CollectionIT.createCollection();

        String content = "this is content";
        String directory = UUID.randomUUID().toString();
        String path = directory + "/data.json";

        Response createResponse = given().body(content).post(
                Configuration.getBaseUrl() + "/content/" + description.name + "?uri=" + path);
        createResponse.then().assertThat().statusCode(200);


        String updateContent = "This content has been updated";
        Response updateResponse = given().body(updateContent).post(
                Configuration.getBaseUrl() + "/content/" + description.name + "?uri=" + path);
        updateResponse.then().assertThat().statusCode(200);

        Response getResponse = get(Configuration.getBaseUrl() + "/content/" + description.name + "?uri=" + path);
        getResponse.then().assertThat().statusCode(200);

        assertEquals(updateContent, getResponse.asString());
    }

    @Test
    public void shouldUpdateApprovedContent() {
        CollectionDescription description = CollectionIT.createCollection();

        String content = "this is content";
        String directory = UUID.randomUUID().toString();
        String path = directory + "/data.json";

        Response createResponse = given().body(content).post(
                Configuration.getBaseUrl() + "/content/" + description.name + "?uri=" + path);
        createResponse.then().assertThat().statusCode(200);

        ApproveIT.approve(description.name, path);

        String updateContent = "This content has been updated";
        Response updateResponse = given().body(updateContent).post(
                Configuration.getBaseUrl() + "/content/" + description.name + "?uri=" + path);
        updateResponse.then().assertThat().statusCode(200);

        Response getResponse = get(Configuration.getBaseUrl() + "/content/" + description.name + "?uri=" + path);
        getResponse.then().assertThat().statusCode(200);

        assertEquals(updateContent, getResponse.asString());
    }
}
