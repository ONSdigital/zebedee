package com.github.onsdigital.zebedee.keyring.io;

import com.github.onsdigital.zebedee.keyring.CollectionKey;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import static com.github.onsdigital.zebedee.keyring.io.CollectionKeySerializer.COLLECTION_ID_FIELD;
import static com.github.onsdigital.zebedee.keyring.io.CollectionKeySerializer.FIELD_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.io.CollectionKeySerializer.FIELD_MISSING_ERR;
import static com.github.onsdigital.zebedee.keyring.io.CollectionKeySerializer.SECRET_KEY_FIELD;
import static java.text.MessageFormat.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

public class CollectionKeySerializerTest {

    static final String TEST_COLLECTION_ID = "666";

    private CollectionKeySerializer serializer;

    @Mock
    private JsonElement jsonElement, innerJsonElement;

    private JsonObject jsonObject;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        serializer = new CollectionKeySerializer();

        this.jsonObject = new JsonObject();
    }

    @Test(expected = JsonParseException.class)
    public void testDeserialize_shouldThrowException_ifCollectionIDNotFound() throws Exception {
        when(jsonElement.getAsJsonObject())
                .thenReturn(jsonObject);

        CollectionKey actual = null;
        try {
            actual = serializer.deserialize(jsonElement, null, null);
        } catch (JsonParseException ex) {
            assertThat(ex.getMessage(), equalTo(format(FIELD_MISSING_ERR, COLLECTION_ID_FIELD)));
            assertThat(actual, is(nullValue()));
            throw ex;
        }
    }


    @Test(expected = JsonParseException.class)
    public void testDeserialize_shouldThrowException_ifCollectionIDEmpty() throws Exception {
        when(jsonElement.getAsJsonObject())
                .thenReturn(jsonObject);

        jsonObject.addProperty(COLLECTION_ID_FIELD, "");

        CollectionKey actual = null;
        try {
            actual = serializer.deserialize(jsonElement, null, null);
        } catch (JsonParseException ex) {
            assertThat(ex.getMessage(), equalTo(format(FIELD_EMPTY_ERR, COLLECTION_ID_FIELD)));
            assertThat(actual, is(nullValue()));
            throw ex;
        }
    }

    @Test(expected = JsonParseException.class)
    public void testDeserialize_shouldThrowException_ifSecretKeyNotFound() throws Exception {
        when(jsonElement.getAsJsonObject())
                .thenReturn(jsonObject);

        jsonObject.addProperty(COLLECTION_ID_FIELD, TEST_COLLECTION_ID);

        CollectionKey actual = null;
        try {
            actual = serializer.deserialize(jsonElement, null, null);
        } catch (JsonParseException ex) {
            assertThat(ex.getMessage(), equalTo(format(FIELD_MISSING_ERR, SECRET_KEY_FIELD)));
            assertThat(actual, is(nullValue()));
            throw ex;
        }
    }

    @Test(expected = JsonParseException.class)
    public void testDeserialize_shouldThrowException_ifSecretKeyEmpty() throws Exception {
        when(jsonElement.getAsJsonObject())
                .thenReturn(jsonObject);

        jsonObject.addProperty(COLLECTION_ID_FIELD, TEST_COLLECTION_ID);
        jsonObject.addProperty(SECRET_KEY_FIELD, "");

        CollectionKey actual = null;
        try {
            actual = serializer.deserialize(jsonElement, null, null);
        } catch (JsonParseException ex) {
            assertThat(ex.getMessage(), equalTo(format(FIELD_EMPTY_ERR, SECRET_KEY_FIELD)));
            assertThat(actual, is(nullValue()));
            throw ex;
        }
    }

    @Test
    public void testDeserialize_Success() throws Exception {
        when(jsonElement.getAsJsonObject())
                .thenReturn(jsonObject);

        jsonObject.addProperty(COLLECTION_ID_FIELD, TEST_COLLECTION_ID);

        SecretKey key = KeyGenerator.getInstance("AES").generateKey();
        String keyAsStr = new String(Base64.getEncoder().encode(key.getEncoded()));
        jsonObject.addProperty(SECRET_KEY_FIELD, keyAsStr);

        CollectionKey actual = serializer.deserialize(jsonElement, null, null);

        assertThat(actual, equalTo(new CollectionKey(TEST_COLLECTION_ID, key)));
    }

    @Test
    public void testSerialize_Success() throws Exception {
        SecretKey key = KeyGenerator.getInstance("AES").generateKey();
        CollectionKey input = new CollectionKey(TEST_COLLECTION_ID, key);

        JsonElement element = serializer.serialize(input, null, null);

        assertThat(element, is(notNullValue()));

        JsonObject jObj = element.getAsJsonObject();
        String actualCollectionID = jObj.get(COLLECTION_ID_FIELD).getAsString();

        String secretKeyStr = jObj.get(SECRET_KEY_FIELD).getAsString();
        assertThat(secretKeyStr, is(notNullValue()));

        byte[] keyBytes = Base64.getDecoder().decode(secretKeyStr);
        SecretKey actualSecretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");

        assertThat(input, equalTo(new CollectionKey(actualCollectionID, actualSecretKey))) ;
    }
}
