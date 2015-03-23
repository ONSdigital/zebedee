package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.model.Configuration;
import com.github.onsdigital.zebedee.json.User;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class UsersIT {

    @Test
    public void shouldNotCreateDuplicateUser() throws UnsupportedEncodingException {

        // Given
        // A duplicate email address:
        String random = Random.password(8);
        String name = "Mr Rusty";
        String email = "mr.rusty." + Random.id() + "@magic.roundabout.com";
        User user = new User();
        user.name = name;
        user.email = email;

        // When
        // We attempt to create the user
        Response createUserResponse = given().body(Serialiser.serialise(user)).post(Configuration.getBaseUrl() + "/users");
        Response postResponse = given().body(Serialiser.serialise(user)).post(Configuration.getBaseUrl() + "/users");

        // Then
        // The user should not be created twice
        createUserResponse.then().assertThat().statusCode(200);
        postResponse.then().assertThat().statusCode(409);
    }

    @Test
    public void shouldCreateAndReadUser() throws UnsupportedEncodingException {

        // Given
        // A new user
        String name = "Blue Cat";
        String email = "blue.cat." + Random.id() + "@magic.roundabout.com";
        User user = new User();
        user.name = name;
        user.email = email;

        // When
        // We create the user and read back the details
        Response postResponse = given().body(Serialiser.serialise(user)).post(Configuration.getBaseUrl() + "/users");
        Response getResponse = given().param("email", email).get(Configuration.getBaseUrl() + "/users");

        // Then
        ValidatableResponse post = postResponse.then();
        ValidatableResponse get = getResponse.then();

        // The user should be created
        post.assertThat().statusCode(200);
        get.assertThat().statusCode(200);

        // We should get the expected user details
        get.body("email", is(email));
        get.body("name", is(name));
    }
}
