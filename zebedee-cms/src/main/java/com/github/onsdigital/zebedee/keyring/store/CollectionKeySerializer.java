package com.github.onsdigital.zebedee.keyring.store;

import com.github.onsdigital.zebedee.keyring.CollectionKey;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Type;
import java.util.Base64;

/**
 * CollectionKeySerializer is custom {@link com.google.gson.Gson} marshall/unmarshaller for {@link CollectionKey}
 * objects. Implements both {@link JsonSerializer} and {@link JsonDeserializer} interfaces.
 */
public class CollectionKeySerializer implements JsonDeserializer<CollectionKey>, JsonSerializer<CollectionKey> {

    private static final String SECRET_KEY_FIELD = "secret_key";
    private static final String COLLECTION_ID_FIELD = "collection_id";
    private static final String ENCRYPTION_ALGORITHM = "AES";

    @Override
    public CollectionKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jObj = json.getAsJsonObject();

        byte[] keyBytes = Base64.getDecoder().decode(jObj.get(SECRET_KEY_FIELD).getAsString());
        SecretKey key = new SecretKeySpec(keyBytes, 0, keyBytes.length, ENCRYPTION_ALGORITHM);

        String collectionID = jObj.get(COLLECTION_ID_FIELD).getAsString();
        return new CollectionKey(collectionID, key);
    }

    @Override
    public JsonElement serialize(CollectionKey src, Type typeOfSrc, JsonSerializationContext context) {
        byte[] encodedBytes = Base64.getEncoder().encode(src.getSecretKey().getEncoded());

        JsonObject jObj = new JsonObject();
        jObj.addProperty(COLLECTION_ID_FIELD, src.getCollectionID());
        jObj.addProperty(SECRET_KEY_FIELD, new String(encodedBytes));

        return jObj;
    }
}
