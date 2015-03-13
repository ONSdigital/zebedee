package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.Configuration;
import com.jayway.restassured.response.Response;

import static com.jayway.restassured.RestAssured.post;

public class ApproveIT {
    public static void approve(String collectionName, String path) {
        Response postResponse = post(Configuration.getBaseUrl() + "/approve/" + collectionName + "?uri=" + path);
        postResponse.then().assertThat().statusCode(200);
    }
}
