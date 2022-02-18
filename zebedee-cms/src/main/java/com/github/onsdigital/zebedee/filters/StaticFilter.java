package com.github.onsdigital.zebedee.filters;

import com.github.davidcarboni.restolino.framework.PreFilter;
import com.github.davidcarboni.restolino.framework.Priority;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Captures requests with a file extension as they do not get routed to the API.
 */
@Priority(3)
public class StaticFilter implements PreFilter {

    @Override
    public boolean filter(HttpServletRequest req, HttpServletResponse res) {
        return !isStaticContentRequest(req);
    }

    /**
     * A request is considered to be a static content request if there is a file
     * extension present.
     *
     * @param req
     *            The request.
     * @return If the result of {@link org.apache.commons.io.FilenameUtils#getExtension(String)} is
     *         not blank, true.
     */
    private boolean isStaticContentRequest(HttpServletRequest req) {
        String requestURI = req.getRequestURI();
        String extension = FilenameUtils.getExtension(requestURI);
        return StringUtils.isNotBlank(extension);
    }
}
