package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import static com.github.onsdigital.zebedee.api.TeamsReport.TEAM_COL;
import static com.github.onsdigital.zebedee.api.TeamsReport.TEAM_COL_INDEX;
import static com.github.onsdigital.zebedee.api.TeamsReport.USER_COL;
import static com.github.onsdigital.zebedee.api.TeamsReport.USER_COL_INDEX;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
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
    private SessionsService sessionsService;

    private TeamsReport api;

    private Team teamA, teamB, teamC;

    @Override
    protected void customSetUp() throws Exception {
        api = new TeamsReport();

        ServiceSupplier<TeamsService> teamsServiceSupplier = () -> teamsService;
        ServiceSupplier<SessionsService> sessionsServiceSupplier = () -> sessionsService;

        ReflectionTestUtils.setField(api, "teamsServiceSupplier", teamsServiceSupplier);
        ReflectionTestUtils.setField(api, "serviceServiceSupplier", sessionsServiceSupplier);
    }

    @Override
    protected Object getAPIName() {
        return "TeamsReport";
    }

    @Test(expected = UnauthorizedException.class)
    public void getReport_ShouldProbegateExceptionFromTeamsService() throws Exception {
        when(sessionsService.get(mockRequest))
                .thenReturn(session);
        when(teamsService.getTeamMembersSummary(any()))
                .thenThrow(new UnauthorizedException(""));

        try {
            api.getReport(mockRequest, mockResponse);
        } catch (UnauthorizedException e) {
            verify(sessionsService, times(1)).get(mockRequest);
            verify(teamsService, times(1)).getTeamMembersSummary(session);
            verify(mockResponse, never()).getOutputStream();
            throw e;
        }
    }

    @Test
    public void getReport_Success() throws Exception {
        mockResponse = new MockHttpServletResponse();

        List<AbstractMap.SimpleEntry<String, String>> teamUserMapping = new ArrayList<>();
        teamUserMapping.add(create("ATeam", "userA"));
        teamUserMapping.add(create("ATeam", "userB"));
        teamUserMapping.add(create("ATeam", "userC"));
        teamUserMapping.add(create("BTeam", ""));
        teamUserMapping.add(create("CTeam", "userA"));
        teamUserMapping.add(create("CTeam", "userC"));

        when(sessionsService.get(mockRequest))
                .thenReturn(session);
        when(teamsService.getTeamMembersSummary(session))
                .thenReturn(teamUserMapping);

        api.getReport(mockRequest, mockResponse);

        MockHttpServletResponse response = (MockHttpServletResponse) mockResponse;
        InputStream in = new ByteArrayInputStream(response.getContentAsByteArray());

        HSSFWorkbook result = new HSSFWorkbook(in);
        HSSFSheet sheet = result.getSheetAt(0);

        assertThat(response.getStatus(), equalTo(HttpStatus.OK.value()));
        assertThat(response.getContentType(), equalTo(MediaType.APPLICATION_OCTET_STREAM_VALUE));

        verify(sessionsService, times(1)).get(mockRequest);
        verify(teamsService, times(1)).getTeamMembersSummary(session);

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
