package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.model.Configuration;
import com.jayway.restassured.response.Response;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;

public class LoginIT {

    public static final String tokenHeader = "x-florence-token";

    public static User createUser() {
        User user = new User();
        user.name = "Mr Rusty";
        user.email = "mr.rusty@magic.roundabout.com";

        Response postResponse = given().body(Serialiser.serialise(user)).post(Configuration.getBaseUrl() + "/users");
        postResponse.then().assertThat().statusCode(200);

        return user;
    }

    public static String setPassword(User user) {

        String password = Random.password(8);
        Credentials credentials = new Credentials();
        credentials.email = user.email;
        credentials.password = password;

        return password;
    }

    public static void setPassword(Credentials credentials) {
        Response postResponse = given().body(Serialiser.serialise(credentials)).post(Configuration.getBaseUrl() + "/password");
        postResponse.then().assertThat().statusCode(200);
    }

    public static String login(Credentials credentials) {
        Response postResponse = given().body(Serialiser.serialise(credentials)).post(Configuration.getBaseUrl() + "/login");
        postResponse.then().assertThat().statusCode(200);
        return postResponse.asString();
    }

    public static Credentials defaultCredentials() {
        Credentials credentials = new Credentials();
        credentials.email = "florence@magicroundabout.ons.gov.uk";
        credentials.password = "Doug4l";
        return credentials;
    }

    @Test
    public void shouldReturn400IfNoEmailSpecified() {

        // Given
        Credentials credentials = new Credentials();
        credentials.email = null;
        String json = Serialiser.serialise(credentials);
        CollectionDescription description = CollectionIT.createCollection();

        Response postResponse = given().body(json).post(Configuration.getBaseUrl() + "/login");
        postResponse.then().assertThat().statusCode(400);
    }

    @Test
    public void shouldReturn401IfPasswordIncorrect() {

        // Given
        Credentials credentials = new Credentials();
        credentials.email = null;
        String json = Serialiser.serialise(credentials);
        CollectionDescription description = CollectionIT.createCollection();

        Response postResponse = given().body(json).post(Configuration.getBaseUrl() + "/login");
        postResponse.then().assertThat().statusCode(400);
    }

    @Test
    public void shouldReturn200WithValidCollectionName() {

        CollectionDescription description = CollectionIT.createCollection();

        Response response = given()
                .get(Configuration.getBaseUrl() + "/browse/" + description.name);
        response.then().assertThat().statusCode(200);
    }
}
