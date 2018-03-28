package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.model.ServiceAccount;

import java.io.IOException;
import java.io.InputStream;

public interface ServiceStore {

     ServiceAccount get(String id) throws IOException;

     ServiceAccount store(String token, InputStream service) throws IOException;
}
