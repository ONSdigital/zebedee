package com.github.onsdigital.zebedee.keyring.store;

import com.github.onsdigital.zebedee.keyring.CollectionKey;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Type;
import java.util.Base64;

import static java.text.MessageFormat.format;

/**
 * CollectionKeySerializer is custom {@link com.google.gson.Gson} marshall/unmarshaller for {@link CollectionKey}
 * objects. Implements both {@link JsonSerializer} and {@link JsonDeserializer} interfaces.
 */
public class CollectionKeySerializer implements JsonDeserializer<CollectionKey>, JsonSerializer<CollectionKey> {

    static final String SECRET_KEY_FIELD = "secret_key";
    static final String COLLECTION_ID_FIELD = "collection_id";
    static final String ENCRYPTION_ALGORITHM = "AES";
    static final String FIELD_MISSING_ERR = "error deserializing CollectionKey expected {0} field but none found";
    static final String FIELD_EMPTY_ERR = "error deserializing CollectionKey expected {0} field but was empty";

    @Override
    public CollectionKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jObj = json.getAsJsonObject();

        JsonElement element = jObj.get(COLLECTION_ID_FIELD);
        if (element == null) {
            throw new JsonParseException(format(FIELD_MISSING_ERR, COLLECTION_ID_FIELD));
        }

        String collectionID = element.getAsString();
        if (StringUtils.isEmpty(collectionID)) {
            throw new JsonParseException(format(FIELD_EMPTY_ERR, COLLECTION_ID_FIELD));
        }

        element = jObj.get(SECRET_KEY_FIELD);
        if (element == null) {
            throw new JsonParseException(format(FIELD_MISSING_ERR, SECRET_KEY_FIELD));
        }

        String secretKeyStr = element.getAsString();
        if (StringUtils.isEmpty(secretKeyStr)) {
            throw new JsonParseException(format(FIELD_EMPTY_ERR, SECRET_KEY_FIELD));
        }

        byte[] keyBytes = Base64.getDecoder().decode(secretKeyStr);
        SecretKey key = new SecretKeySpec(keyBytes, 0, keyBytes.length, ENCRYPTION_ALGORITHM);

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
