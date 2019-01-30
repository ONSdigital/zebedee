package com.github.onsdigital.zebedee.content.util;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.onsdigital.zebedee.ReaderFeatureFlags.readerFeatureFlags;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logTrace;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logWarn;

/**
 * Created by bren on 09/06/15.
 */
class PageTypeResolver implements JsonDeserializer<Page> {

    private static Map<PageType, Class> contentClasses = new HashMap<PageType, Class>();
    private static Function<Map.Entry<PageType, Class>, String> contentTypeNameFunc = (e) -> e.getKey().getDisplayName();
    private static PageTypeResolver instance = null;

    private boolean datasetImportEnabled;
    private Set<PageType> datasetImportPageTypes;
    private Predicate<PageType> isDatasetImportPageType;

    private PageTypeResolver(boolean datasetImportEnabled, Predicate<PageType> isDatasetImportPageType) {
        this.datasetImportEnabled = datasetImportEnabled;
        this.isDatasetImportPageType = isDatasetImportPageType;
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

            // FIXME CMD feature
            if (!datasetImportEnabled && isDatasetImportPageType.test(contentType)) {
                logWarn("PageType invalid feature EnableDatasetImport disabled. Enable this feature by updating the Zebedee configuration")
                        .addParameter("pageType", contentType.getDisplayName())
                        .log();
                throw new JsonParseException("Invalid page type");
            }

            Class<Page> pageClass = contentClasses.get(contentType);
            if (pageClass == null) {
                throw new RuntimeException("Could find content object for " + type);
            }
            Page content = context.deserialize(json, pageClass);
            return content;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }


    public static PageTypeResolver getInstance() {
        if (instance == null) {
            synchronized (PageTypeResolver.class) {
                if (instance == null) {
                    logInfo("initialising PageTypeResolver instance").log();
                    boolean isDatasetImportEnabled = readerFeatureFlags().isEnableDatasetImport();
                    Predicate<PageType> isDatasetImportPageType = (p) -> readerFeatureFlags().datasetImportPageTypes().contains(p);

                    registerContentTypes();

                    logInfo("registered content types")
                            .parameter("contentTypes", contentClasses.entrySet().stream(), contentTypeNameFunc)
                            .log();

                    instance = new PageTypeResolver(isDatasetImportEnabled, isDatasetImportPageType);
                }
            }
        }
        return instance;
    }

    private static void registerContentTypes() {
        try {

            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder().addUrls(
                    PageTypeResolver.class.getProtectionDomain().getCodeSource().getLocation());
            configurationBuilder.addClassLoader(PageTypeResolver.class.getClassLoader());
            Set<Class<? extends Page>> classes = new Reflections(configurationBuilder).getSubTypesOf(Page.class);

            for (Class<? extends Page> contentClass : classes) {
                String className = contentClass.getSimpleName();
                boolean _abstract = Modifier.isAbstract(contentClass.getModifiers());
                if (_abstract) {
                    logTrace("Skipping registering abstract content").addParameter("type", className).log();
                    continue;
                }

                try {
                    Page contentInstance = contentClass.newInstance();
                    contentClasses.put(contentInstance.getType(), contentClass);
                } catch (InstantiationException e) {
                    logError(e, "Failed to instantiate content type").addParameter("pageType", className).log();
                }
            }
        } catch (Exception e) {
            logError(e, "Failed initializing content types").log();
            throw new RuntimeException("Failed initializing request handlers", e);
        }
    }
}