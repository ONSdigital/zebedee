package com.github.onsdigital.zebedee.audit;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

/**
 * Created by iankent on 22/02/2016.
 */
public class Audit {
    private static final Logger logger = Logger.getLogger("audit");

    public static void log(HttpServletRequest request, String msg, Object ...args) {
        String remoteIP = "<local>";

        if(request != null) {
            remoteIP = request.getHeader("X-Forwarded-For");
            if (remoteIP == null || remoteIP.length() == 0) {
                remoteIP = request.getRemoteUser();
            }
        }

        String s = String.format("[%s] " + msg, remoteIP, args);
        logger.info(s);
    }
}
