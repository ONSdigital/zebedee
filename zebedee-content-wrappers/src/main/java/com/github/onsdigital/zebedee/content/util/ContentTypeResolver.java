package com.github.onsdigital.zebedee.content.util;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.base.ContentType;
import com.google.gson.*;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by bren on 09/06/15.
 */
class ContentTypeResolver implements JsonDeserializer<Content> {

    private static Map<ContentType, Class> contentClasses = new HashMap<ContentType, Class>();

    static {
        registerContentTypes();
    }

    @Override
    public Content deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        JsonElement jsonType = jsonObject.get("type");
        if (jsonType == null) {
            return null;
        }

        String type = jsonType.getAsString();

        try {
            ContentType contentType = ContentType.valueOf(type);
            Class<Content> pageClass = contentClasses.get(contentType);
            if(pageClass == null) {
                throw new RuntimeException("Could find content object for " + type);
            }
            Content content = context.deserialize(json, pageClass);
            return content;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }


    private static void registerContentTypes() {
        System.out.println("Resolving page types");
        try {

            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder().addUrls(ContentTypeResolver.class.getProtectionDomain().getCodeSource().getLocation());
            configurationBuilder.addClassLoader(ContentTypeResolver.class.getClassLoader());
            Set<Class<? extends Content>> classes = new Reflections(configurationBuilder).getSubTypesOf(Content.class);

            for (Class<? extends Content> contentClass : classes) {
                String className = contentClass.getSimpleName();
                boolean _abstract = Modifier.isAbstract(contentClass.getModifiers());
                if (_abstract) {
                    System.out.println("Skipping registering abstract content type " + className);
                    continue;
                }
                Content contentInstance = contentClass.newInstance();
                System.out.println("Registering content type, Page type : " +  contentInstance.getType()  + ":" +  className);
                contentClasses.put(contentInstance.getType(), contentClass);
            }
        } catch (Exception e) {
            System.err.println("Failed initializing content types");
            throw new RuntimeException("Failed initializing request handlers", e);
        }

    }

}
