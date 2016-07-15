package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.AssociatedFile;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.service.MathjaxEquationService;
import com.github.onsdigital.zebedee.service.SvgService;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.commons.fileupload.FileUploadException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Given some Tex equation input, return the equation in SVG format.
 */
@Api
public class Equation {

    @POST
    public boolean renderEquation(
            HttpServletRequest request,
            HttpServletResponse response,
            com.github.onsdigital.zebedee.content.page.statistics.document.figure.equation.Equation equation
    ) throws IOException, ZebedeeException, FileUploadException, TranscoderException {

        Session session = Root.zebedee.sessions.get(request);
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        String uri = request.getParameter("uri");

        Path path = Paths.get(uri);
        CollectionWriter collectionWriter = new ZebedeeCollectionWriter(Root.zebedee, collection, session);

        String svgOuput = generateEquationSvg(request, equation, session, collection, path);
        generatePngOutput(equation, path, collectionWriter, svgOuput);

        writeEquationJsonToCollection(request, equation, session, collection, uri);

        return true;
    }

    @DELETE
    public boolean deleteEquation(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException, ZebedeeException, FileUploadException, TranscoderException {

        Session session = Root.zebedee.sessions.get(request);
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        String uri = request.getParameter("uri");

        Path path = Paths.get(uri);

        CollectionReader collectionReader = new ZebedeeCollectionReader(Root.zebedee, collection, session);
        com.github.onsdigital.zebedee.content.page.statistics.document.figure.equation.Equation equation = (com.github.onsdigital.zebedee.content.page.statistics.document.figure.equation.Equation) collectionReader.getContent(uri);

        // delete each associated file.
        for (AssociatedFile file : equation.getFiles()) {
            Path pathToDelete = path.getParent().resolve(file.getFilename());

            String uriToDelete = pathToDelete.toString();
            deleteFile(request, session, collection, uriToDelete);
        }

        // delete the Json
        deleteFile(request, session, collection, uri + ".json");

        return true;
    }

    public void deleteFile(HttpServletRequest request, Session session, com.github.onsdigital.zebedee.model.Collection collection, String uriToDelete) throws IOException, ZebedeeException {
        boolean result = Root.zebedee.collections.deleteContent(collection, uriToDelete, session);
        if (result) {
            Audit.Event.CONTENT_DELETED
                    .parameters()
                    .host(request)
                    .collection(collection)
                    .content(uriToDelete)
                    .user(session.email).log();
        }
    }

    public void generatePngOutput(
            com.github.onsdigital.zebedee.content.page.statistics.document.figure.equation.Equation equation,
            Path path, CollectionWriter collectionWriter,
            String svgOuput
    ) throws IOException, TranscoderException, BadRequestException {

        Path pngPath = path.getParent().resolve(equation.getFilename() + ".png");

        try (InputStream inputStream = new ByteArrayInputStream(svgOuput.getBytes());
             OutputStream outputStream = collectionWriter.getInProgress().getOutputStream(pngPath.toString())) {

            SvgService.convertSvgToPng(inputStream, outputStream);

            String fileName = pngPath.getFileName().toString();
            addAssociatedFileToEquation(equation, "generated-png", fileName, "png");
        }
    }

    public String generateEquationSvg(
            HttpServletRequest request,
            com.github.onsdigital.zebedee.content.page.statistics.document.figure.equation.Equation equation,
            Session session, com.github.onsdigital.zebedee.model.Collection collection,
            Path path
    ) throws IOException, ZebedeeException, FileUploadException {
        Path svgPath = path.getParent().resolve(equation.getFilename() + ".svg");
        String svgOuput = MathjaxEquationService.render(equation.getContent());

        try (InputStream inputStream = new ByteArrayInputStream(svgOuput.getBytes())) {
            boolean validateJson = false;
            Root.zebedee.collections.writeContent(collection, svgPath.toString(), session, request, inputStream, false, CollectionEventType.COLLECTION_FILE_SAVED, validateJson);

            String fileName = svgPath.getFileName().toString();
            addAssociatedFileToEquation(equation, "generated-svg", fileName, "svg");
        }
        return svgOuput;
    }

    private void addAssociatedFileToEquation(
            com.github.onsdigital.zebedee.content.page.statistics.document.figure.equation.Equation equation,
            String fileType,
            String fileName,
            String fileExtension
    ) {
        if (equation.getFiles() == null)
            equation.setFiles(new ArrayList<>());

        boolean fileAlreadyListed = false;
        for (AssociatedFile file : equation.getFiles()) {
            if (file.getFilename().equals(fileName)) {
                fileAlreadyListed = true;
                break;
            }
        }

        if (!fileAlreadyListed) {
            AssociatedFile file = new AssociatedFile();
            file.setFilename(fileName);
            file.setFileType(fileExtension);
            file.setType(fileType);
            equation.getFiles().add(file);
        }
    }

    public void writeEquationJsonToCollection(
            HttpServletRequest request,
            com.github.onsdigital.zebedee.content.page.statistics.document.figure.equation.Equation equation,
            Session session, com.github.onsdigital.zebedee.model.Collection collection,
            String uri
    ) throws IOException, ZebedeeException, FileUploadException {
        // write the equation json to the collection
        String serialisedEquation = ContentUtil.serialise(equation);
        try (InputStream inputStream = new ByteArrayInputStream(serialisedEquation.getBytes())) {
            boolean validateJson = true;
            Root.zebedee.collections.writeContent(collection, uri, session, request, inputStream, false, CollectionEventType.COLLECTION_FILE_SAVED, validateJson);
            Audit.Event.CONTENT_OVERWRITTEN
                    .parameters()
                    .host(request)
                    .collection(collection)
                    .content(uri)
                    .user(session.email).log();
        }
    }
}