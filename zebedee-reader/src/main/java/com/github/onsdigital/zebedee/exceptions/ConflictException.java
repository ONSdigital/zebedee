package com.github.onsdigital.zebedee.exceptions;

import org.eclipse.jetty.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by david on 23/04/15.
 */
public class ConflictException extends ZebedeeExceptionWithData {
    public String collectionName;

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT_409);
    }

    public ConflictException(String message, String collectionName) {
        super(message, HttpStatus.CONFLICT_409);
        this.collectionName = collectionName;
        
        HashMap<String, String> data = new HashMap<>();
        data.put("collectionName", collectionName);
        this.data = data;
    }
}
