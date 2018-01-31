package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ForbiddenException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.text.MessageFormat.format;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

/**
 * API endpoint for generating Team members XLS report.
 */
@Api
public class TeamsReport {

    private static final String CONTENT_DISPOSITION_VALUE = "attachment;filename=teams-report-{0}.xls";
    private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";

    static final DateFormat DF = new SimpleDateFormat("dd-MM-yyyy");
    static final String TEAM_COL = "Team name";
    static final String USER_COL = "User";
    static final int TEAM_COL_INDEX = 0;
    static final int USER_COL_INDEX = 1;
    static final String SHEET_TITLE = "Teams Summary {0}";

    private ServiceSupplier<TeamsService> teamsServiceSupplier = () -> Root.zebedee.getTeamsService();
    private ServiceSupplier<SessionsService> serviceServiceSupplier = () -> Root.zebedee.getSessionsService();

    /**
     * Generate an on the fly Team memmbers report XLS and write to the response as a file download.
     *
     * @param request  the client {@link HttpServletRequest}
     * @param response the {@link HttpServletResponse} to the client.
     * @throws IOException           error while generating teams report.
     * @throws BadRequestException   error while generating teams report.
     * @throws UnauthorizedException user not authorised to request teams report.
     */
    @GET
    public void getReport(HttpServletRequest request, HttpServletResponse response) throws IOException,
            BadRequestException, UnauthorizedException, ForbiddenException {
        Session session = serviceServiceSupplier.getService().get(request);
        response.setContentType(APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(CONTENT_DISPOSITION_HEADER, format(CONTENT_DISPOSITION_VALUE, getDateString()));
        response.setStatus(HttpStatus.OK.value());
        createWorkbook(session, response);
    }

    private void createWorkbook(Session session, HttpServletResponse response) throws IOException,
            UnauthorizedException, ForbiddenException {
        List<AbstractMap.SimpleEntry<String, String>> teamMembersMapping = teamsServiceSupplier
                .getService()
                .getTeamMembersSummary(session);

        final AtomicInteger rowIndex = new AtomicInteger(0);
        String sheetName = format(SHEET_TITLE, getDateString());

        try (HSSFWorkbook workbook = new HSSFWorkbook()) {
            HSSFSheet sheet = workbook.createSheet(sheetName);

            // Create the title row.
            createTeamUserCells(sheet, rowIndex, TEAM_COL, USER_COL);

            teamMembersMapping.stream()
                    .forEach(entry -> createTeamUserCells(sheet, rowIndex, entry.getKey(), entry.getValue()));
            formatWorkbook(workbook, sheetName).write(response.getOutputStream());
        }
    }

    private HSSFWorkbook formatWorkbook(HSSFWorkbook workbook, String sheetName) {
        HSSFCellStyle style = workbook.createCellStyle();
        HSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        HSSFSheet sheet = workbook.getSheet(sheetName);

        HSSFRow titleRow = sheet.getRow(0);
        titleRow.getCell(TEAM_COL_INDEX).setCellStyle(style);
        titleRow.getCell(USER_COL_INDEX).setCellStyle(style);

        sheet.autoSizeColumn(TEAM_COL_INDEX);
        sheet.autoSizeColumn(USER_COL_INDEX);
        return workbook;
    }

    private void createTeamUserCells(HSSFSheet sheet, AtomicInteger rowIndex, String teamName, String userEmail) {
        HSSFRow row = sheet.createRow(rowIndex.getAndIncrement());

        HSSFCell teamNameCell = row.createCell(TEAM_COL_INDEX);
        teamNameCell.setCellType(Cell.CELL_TYPE_STRING);
        teamNameCell.setCellValue(teamName);

        HSSFCell userEmailCell = row.createCell(USER_COL_INDEX);
        userEmailCell.setCellType(Cell.CELL_TYPE_STRING);
        userEmailCell.setCellValue(userEmail);
    }

    private String getDateString() {
        return DF.format(new Date());
    }
}
