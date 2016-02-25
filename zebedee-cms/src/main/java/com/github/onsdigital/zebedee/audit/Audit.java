package com.github.onsdigital.zebedee.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by iankent on 22/02/2016.
 */
public class Audit {
    private static final Logger logger = LoggerFactory.getLogger("com.github.onsdigital.zebedee.audit");
    private static final String DEFAULT_REMOTE_IP = "<local>";

    public static void log(HttpServletRequest request, String msg, Object ...args) {
        String remoteIP = DEFAULT_REMOTE_IP;

        if(request != null) {
            remoteIP = request.getHeader("X-Forwarded-For");
            if (remoteIP == null || remoteIP.length() == 0) {
                remoteIP = request.getRemoteAddr() != null ? request.getRemoteAddr() : DEFAULT_REMOTE_IP;
            }
        }
        try {
            String formattedMsg = String.format(msg, args);
            String s = String.format("[%s] %s", remoteIP, formattedMsg);
            logger.info(s);
        } catch (Exception ex) {
            // TODO very temp fix - catch any errors thrown while logging and log the cause. DL to revisit and fix properly.
            logger.info("Audit log message creation failed.", ex);
        }
    }
}
