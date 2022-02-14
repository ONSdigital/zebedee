package com.github.onsdigital.zebedee.junit4.rules;

/**
 * RunInThread and other accompanying files are licensed under the MIT
 * license.  Copyright (C) Frank Appel 2016-2021. All rights reserved.
 *
 * Source: https://gist.github.com/fappel/65982e5ea7a6b2fde5a3
 */
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RunInThread {
}
