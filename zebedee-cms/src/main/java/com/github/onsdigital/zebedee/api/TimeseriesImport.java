package com.github.onsdigital.zebedee.api;


import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.json.ApprovalStatus;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Upload a CSV file to update the metadata of timeseries.
 */
@Api
public class TimeseriesImport {
    @POST
    public boolean importTimeseries(HttpServletRequest request, HttpServletResponse response) throws Exception {

        // otherwise the call to get a request parameter will actually consume the body:
        try (InputStream requestBody = request.getInputStream()) {

            Session session = Root.zebedee.getSessionsService().get(request);
            com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);

            CollectionWriter collectionWriter = new ZebedeeCollectionWriter(Root.zebedee, collection, session);

            if (collection.description.approvalStatus == ApprovalStatus.COMPLETE) {
                throw new BadRequestException("This collection has been approved and cannot be saved to.");
            }

            ServletFileUpload upload = Root.zebedee.getCollections().getServletFileUpload();

            boolean collectionUpdated = false;
            try {
                for (FileItem item : upload.parseRequest(request)) {
                    try (InputStream inputStream = item.getInputStream()) {
                        collectionWriter.getRoot().write(inputStream, item.getName());

                        // update the collection json with the file upload.
                        if (collection.description.timeseriesImportFiles == null)
                            collection.description.timeseriesImportFiles = new ArrayList<>();
                        collection.description.timeseriesImportFiles.add(item.getName());
                        collectionUpdated = true;
                    }
                }
            } catch (Exception e) {
                throw new IOException("Error processing uploaded file", e);
            }

            if (collectionUpdated) {
                collection.save();
            }

            return true;
        }
    }
}
