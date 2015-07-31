package com.github.onsdigital.zebedee.reader.api;

import com.github.onsdigital.zebedee.reader.util.Authoriser;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * Created by bren on 31/07/15.
 * <p>
 * This class checks to see if Zebedee Cms is running to authorize collection views, if not serves published content
 */
public class Permissions {

    private static Authoriser authoriser;

    static {
        registerAuthoriser();
    }

    /*Checks to see if a collection is requested requested collection is permitted to be seen*/
    public static void authorise(HttpServletRequest request) {
        if (authoriser == null) {
        }
    }



    private static void registerAuthoriser() {
        System.out.println("Checking authorising service");
        try {

            ConfigurationBuilder configurationBuilder = new ConfigurationBuilder().addUrls(Permissions.class.getProtectionDomain().getCodeSource().getLocation());
            configurationBuilder.addClassLoader(Permissions.class.getClassLoader());
            Set<Class<? extends Authoriser>> classes = new Reflections(configurationBuilder).getSubTypesOf(Authoriser.class);

            for (Class<? extends Authoriser> contentClass : classes) {
                String className = contentClass.getSimpleName();
                boolean _abstract = Modifier.isAbstract(contentClass.getModifiers());
                if (_abstract) {
                    System.out.println("Skipping registering abstract authoriser" + className);
                    continue;
                }
                Authoriser authoriserInstance = contentClass.newInstance();
                System.out.println("Registering "  +  className + " for authorising collection views");
                authoriser = authoriserInstance;
                return;
            }
            System.out.println("No authoriser found , will only serve published content");
        } catch (Exception e) {
            System.err.println("Failed initializing collection view authoriser");
            throw new RuntimeException("Failed initializing collection view authoriser", e);
        }

    }


}
