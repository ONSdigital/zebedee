package com.github.onsdigital.zebedee.content.util;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
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
class PageTypeResolver implements JsonDeserializer<Page> {

    private static Map<PageType, Class> contentClasses = new HashMap<PageType, Class>();

    static {
        registerContentTypes();
    }

    private static void registerContentTypes() {
        System.out.println("Resolving page types");
        try {

            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder().addUrls(PageTypeResolver.class.getProtectionDomain().getCodeSource().getLocation());
            configurationBuilder.addClassLoader(PageTypeResolver.class.getClassLoader());
            Set<Class<? extends Page>> classes = new Reflections(configurationBuilder).getSubTypesOf(Page.class);

            for (Class<? extends Page> contentClass : classes) {
                String className = contentClass.getSimpleName();
                boolean _abstract = Modifier.isAbstract(contentClass.getModifiers());
                if (_abstract) {
                    System.out.println("Skipping registering abstract content type " + className);
                    continue;
                }

                try {
                    Page contentInstance = contentClass.newInstance();
                    System.out.println("Registering content type, Page type : " + contentInstance.getType() + ":" + className);
                    contentClasses.put(contentInstance.getType(), contentClass);
                } catch (InstantiationException e) {
                    System.out.println("Failed to instantiate content type, Page type : " + className);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed initializing content types");
            throw new RuntimeException("Failed initializing request handlers", e);
        }
    }

    @Override
    public Page deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        JsonElement jsonType = jsonObject.get("type");
        if (jsonType == null) {
            return null;
        }

        String type = jsonType.getAsString();

        try {
            PageType contentType = PageType.valueOf(type);
            Class<Page> pageClass = contentClasses.get(contentType);
            if(pageClass == null) {
                throw new RuntimeException("Could find content object for " + type);
            }
            Page content = context.deserialize(json, pageClass);
            return content;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }
}