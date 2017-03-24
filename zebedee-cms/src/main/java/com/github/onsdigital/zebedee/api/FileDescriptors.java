package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.util.FileDescriptorMonitor;
import com.github.onsdigital.zebedee.util.FileDescriptorStats;

import javax.ws.rs.GET;

@Api
public class FileDescriptors {

    @GET
    public FileDescriptorStats getOpenFileDescriptorCount() {
        return FileDescriptorMonitor.getStats();
    }
}
