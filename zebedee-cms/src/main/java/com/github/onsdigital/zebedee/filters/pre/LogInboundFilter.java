package com.github.onsdigital.zebedee.filters.pre;

import com.github.davidcarboni.restolino.framework.PreFilter;
import com.github.davidcarboni.restolino.framework.Priority;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

@Priority(1)
public class LogInboundFilter implements PreFilter {

    @Override
    public boolean filter(HttpServletRequest request, HttpServletResponse response) {
        info().beginHTTP(request).log("request received");
        return true;
    }
}
