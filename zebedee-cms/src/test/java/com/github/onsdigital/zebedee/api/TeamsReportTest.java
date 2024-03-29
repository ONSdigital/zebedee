package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import org.apache.http.HttpStatus;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import static com.github.onsdigital.zebedee.api.TeamsReport.TEAM_COL;
import static com.github.onsdigital.zebedee.api.TeamsReport.TEAM_COL_INDEX;
import static com.github.onsdigital.zebedee.api.TeamsReport.USER_COL;
import static com.github.onsdigital.zebedee.api.TeamsReport.USER_COL_INDEX;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verify the behaviour of the {@link TeamsReport} API.
 */
public class TeamsReportTest extends ZebedeeAPIBaseTestCase {

    @Mock
    private TeamsService teamsService;

    @Mock
    private Sessions sessions;

    private TeamsReport api;

    private Team teamA, teamB, teamC;

    @Override
    protected void customSetUp() throws Exception {
        api = new TeamsReport();

        ServiceSupplier<TeamsService> teamsServiceSupplier = () -> teamsService;
        ServiceSupplier<Sessions> sessionsServiceSupplier = () -> sessions;

        ReflectionTestUtils.setField(api, "teamsServiceSupplier", teamsServiceSupplier);
        ReflectionTestUtils.setField(api, "serviceServiceSupplier", sessionsServiceSupplier);
    }

    @Override
    protected Object getAPIName() {
        return "TeamsReport";
    }

    @Test(expected = UnauthorizedException.class)
    public void getReport_ShouldProbegateExceptionFromTeamsService() throws Exception {
        when(sessions.get())
                .thenReturn(mockSession);
        when(teamsService.getTeamMembersSummary(any()))
                .thenThrow(new UnauthorizedException(""));

        try {
            api.getReport(mockRequest, mockResponse);
        } catch (UnauthorizedException e) {
            verify(sessions, times(1)).get();
            verify(teamsService, times(1)).getTeamMembersSummary(mockSession);
            verify(mockResponse, never()).getOutputStream();
            throw e;
        }
    }

    @Test
    public void getReport_Success() throws Exception {
        List<AbstractMap.SimpleEntry<String, String>> teamUserMapping = new ArrayList<>();
        teamUserMapping.add(create("ATeam", "userA"));
        teamUserMapping.add(create("ATeam", "userB"));
        teamUserMapping.add(create("ATeam", "userC"));
        teamUserMapping.add(create("BTeam", ""));
        teamUserMapping.add(create("CTeam", "userA"));
        teamUserMapping.add(create("CTeam", "userC"));

        when(sessions.get())
                .thenReturn(mockSession);
        when(teamsService.getTeamMembersSummary(mockSession))
                .thenReturn(teamUserMapping);

        ByteArrayOutputStream content = new ByteArrayOutputStream(1024);
        when(mockResponse.getOutputStream())
                .thenReturn(new StubServletOutputStream(content));

        api.getReport(mockRequest, mockResponse);

        HSSFWorkbook result = new HSSFWorkbook(new ByteArrayInputStream(content.toByteArray()));
        HSSFSheet sheet = result.getSheetAt(0);

        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_OK);
        verify(mockResponse, times(1)).setContentType(MediaType.APPLICATION_OCTET_STREAM);
        verify(sessions, times(1)).get();
        verify(teamsService, times(1)).getTeamMembersSummary(mockSession);

        // assert that HSSFWorkbook is as expected.
        assertThat(result.getNumberOfSheets(), equalTo(1));
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(teamUserMapping.size() + 1)); // + 1 for the title row.
        assertThat(sheet.getRow(0).getPhysicalNumberOfCells(), equalTo(2));
        assertThat(sheet.getRow(0).getCell(TEAM_COL_INDEX).getStringCellValue(), equalTo(TEAM_COL));
        assertThat(sheet.getRow(0).getCell(USER_COL_INDEX).getStringCellValue(), equalTo(USER_COL));

        for (int i = 0; i < teamUserMapping.size(); i++) {
            HSSFRow row = sheet.getRow(i + 1);

            String expectedTeam = teamUserMapping.get(i).getKey();
            String expectedUser = teamUserMapping.get(i).getValue();

            assertThat("Team Name column incorrect.", row.getCell(TEAM_COL_INDEX).getStringCellValue(),
                    equalTo(expectedTeam));
            assertThat("User column incorrect." + expectedUser, row.getCell(USER_COL_INDEX).getStringCellValue(),
                    equalTo(expectedUser));
        }

    }

    private AbstractMap.SimpleEntry<String, String> create(String team, String user) {
        return new AbstractMap.SimpleEntry<String, String>(team, user);
    }
}
