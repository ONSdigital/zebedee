package com.github.onsdigital.zebedee.service;

import org.apache.hc.core5.http.NameValuePair;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface UploadService {
    void uploadResumableFile(File file, List<NameValuePair> params) throws IOException;
}
