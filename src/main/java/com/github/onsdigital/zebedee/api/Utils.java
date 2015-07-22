package com.github.onsdigital.zebedee.api;


import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.content.page.statistics.document.article.Article;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.*;
import com.github.onsdigital.zebedee.util.Librarian;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.IOUtils;
import org.bouncycastle.util.Strings;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by thomasridd on 15/07/15.
 */
@Api
public class Utils {
    /**
     *
     */
    @GET
    public void utilMethods(HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {

        // Currently let's just
        Session session = Root.zebedee.sessions.get(request);
        if (Root.zebedee.permissions.canEdit(session.email) == false) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return;
        }

        String[] sections = StringUtils.split(request.getRequestURI(), "/");
        if (sections.length == 0) { return ; }
        String util = sections[1];

        // Move function
        if (util.equalsIgnoreCase("move")) {
            String fromUri = request.getParameter("fromUri");
            String toUri = request.getParameter("toUri");
            Root.zebedee.launchpad.moveUri(fromUri, toUri);
            return;

            // Catalogue function
        } else if (util.equalsIgnoreCase("catalogue")) {
            Librarian librarian = new Librarian(Root.zebedee);
            librarian.catalogue();

            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"catalogue.csv\"");

            Path catalogue = librarian.csvOfCatalogue();
            try (OutputStream outputStream = response.getOutputStream(); InputStream inputStream = Files.newInputStream(catalogue))
            {
                IOUtils.copy(inputStream, outputStream);
            }
            return;

            // Full list of internal links
        } else if (util.equalsIgnoreCase("links")) {

            // Checks for broken links
        } else if (util.equalsIgnoreCase("validate")) {

        }
    }
}
