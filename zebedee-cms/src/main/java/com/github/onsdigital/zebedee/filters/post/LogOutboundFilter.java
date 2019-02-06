package com.github.onsdigital.zebedee.filters.post;

import com.github.davidcarboni.restolino.framework.PostFilter;
import com.github.davidcarboni.restolino.framework.Priority;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

@Priority(1)
public class LogOutboundFilter implements PostFilter {

    @Override
    public void filter(HttpServletRequest request, HttpServletResponse response) {
        info().endHTTP(response).log("request complete");
    }
}
