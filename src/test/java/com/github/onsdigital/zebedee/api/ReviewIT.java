package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.model.Configuration;
import com.jayway.restassured.response.Response;

import static com.jayway.restassured.RestAssured.post;

public class ReviewIT {
    public static void review(String collectionName, String path) {
        Response postResponse = post(Configuration.getBaseUrl() + "/review/" + collectionName + "?uri=" + path);
        postResponse.then().assertThat().statusCode(200);
    }
}
