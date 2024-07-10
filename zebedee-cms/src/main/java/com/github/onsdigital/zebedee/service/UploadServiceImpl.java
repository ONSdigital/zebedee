package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dp.uploadservice.api.Client;
import org.apache.hc.core5.http.NameValuePair;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class UploadServiceImpl implements UploadService{

    private final Client client;

    public UploadServiceImpl(Client client) {
        this.client = client;
    }

    @Override
    public void uploadResumableFile(File file, List<NameValuePair> params) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file argument was null");
        }

        if (params == null) {
            throw new IllegalArgumentException("params argument was null");
        }

        this.client.uploadResumableFile(file, params);
    }
}
