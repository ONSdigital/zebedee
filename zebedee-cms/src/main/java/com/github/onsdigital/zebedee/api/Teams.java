package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.ForbiddenException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.teams.model.TeamList;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.IOException;
import java.util.List;

import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;

/**
 * Created by thomasridd on 28/04/15.
 * <p>
 * Endpoint that handles team membership
 * <p>
 * <p>Calls are made to one of
 * <ul><li>{@code /teams}</li>
 * <li>{@code /teams/[teamname]}</li>
 * <li>{@code /teams/[teamname]?email=user@example.com}</li>
 * </ul>
 * </p>
 *
 * @deprecated The teams management in zebedee is deprecated in favour of the dp-identity-api with its JWT based auth
 *             and will be removed after migration of users and teams to the new service.
 */
@Api
@Deprecated
public class Teams {

    private static final String EMAIL_PARAM = "email";

    private Sessions sessionsService;
    private TeamsService teamsService;

    /**
     * Construct a new Teams instance using the default configuration.
     */
    public Teams() {
        this.sessionsService = Root.zebedee.getSessions();
        this.teamsService = Root.zebedee.getTeamsService();
    }

    /**
     * @param sessionsService
     * @param teamsService
     * @param permissionsService
     */
    public Teams(final Sessions sessionsService, final TeamsService teamsService) {
        this.sessionsService = sessionsService;
        this.teamsService = teamsService;
    }

    /**
     * POST {@code /teams/[teamname]} creates a team with name {@code teamname}
     * <p>POST {@code /teams/[teamname]?email=user@example.com} adds a user to the team</p>
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws ConflictException     {@value org.eclipse.jetty.http.HttpStatus#CONFLICT_409} if team already exists
     * @throws UnauthorizedException {@value org.eclipse.jetty.http.HttpStatus#UNAUTHORIZED_401} if user does not have permission
     * @throws NotFoundException     {@value org.eclipse.jetty.http.HttpStatus#NOT_FOUND_404} if team can not be found/created
     */
    @POST
    public boolean post(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ConflictException, UnauthorizedException, NotFoundException, ForbiddenException {

        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            throw new NotFoundException("JWT sessions are enabled: POST /teams is no longer supported");
        }

        String email = request.getParameter(EMAIL_PARAM);
        if (StringUtils.isEmpty(email)) {
            return createTeam(request);
        }

        return addTeamMember(request);
    }

    public boolean createTeam(HttpServletRequest request) throws IOException,
            ConflictException, UnauthorizedException, NotFoundException, ForbiddenException {

        Session session = sessionsService.get();
        String teamName = getTeamName(request);

        teamsService.createTeam(teamName, session);

        Audit.Event.TEAM_CREATED
                .parameters()
                .host(request)
                .team(teamName)
                .actionedBy(session.getEmail())
                .log();
        return true;
    }

    public boolean addTeamMember(HttpServletRequest request) throws UnauthorizedException,
            IOException, NotFoundException, ForbiddenException {
        Session session = sessionsService.get();

        String teamName = getTeamName(request);

        String email = request.getParameter(EMAIL_PARAM);
        Team team = teamsService.findTeam(teamName);

        teamsService.addTeamMember(email, team, session);

        Audit.Event.TEAM_MEMBER_ADDED
                .parameters()
                .host(request)
                .team(teamName)
                .teamMember(email)
                .actionedBy(session.getEmail())
                .log();
        return true;
    }

    /**
     * DELETE {@code /teams/[teamname]} deletes the team with name {@code teamname}
     * <p>DELETE {@code /teams/[teamname]?email=user@example.com} removes a user from the team</p>
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws UnauthorizedException {@value org.eclipse.jetty.http.HttpStatus#UNAUTHORIZED_401} if user doesn't have delete permissions
     * @throws NotFoundException     {@value org.eclipse.jetty.http.HttpStatus#NOT_FOUND_404} if the team doesn't exist / user doesn't exist/ user isn't in this team
     * @throws BadRequestException   {@value org.eclipse.jetty.http.HttpStatus#CONFLICT_409} if the team cannot be deleted for some other reason
     */
    @DELETE
    public boolean delete(HttpServletRequest request, HttpServletResponse response) throws IOException,
            UnauthorizedException, NotFoundException, BadRequestException, ForbiddenException {
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            throw new NotFoundException("JWT sessions are enabled: DELETE /teams is no longer supported");
        }

        String email = request.getParameter(EMAIL_PARAM);
        if (email == null) {
            return deleteTeam(request);
        } else {
            return removeTeamMember(request);
        }
    }

    public boolean deleteTeam(HttpServletRequest request) throws NotFoundException,
            BadRequestException, UnauthorizedException, IOException, ForbiddenException {
        String teamName = getTeamName(request);

        Session session = sessionsService.get();
        Team team = teamsService.findTeam(teamName);
        teamsService.deleteTeam(team, session);

        Audit.Event.TEAM_DELETED
                .parameters()
                .host(request)
                .team(teamName)
                .actionedBy(session.getEmail())
                .log();
        return true;
    }

    public boolean removeTeamMember(HttpServletRequest request)
            throws UnauthorizedException, IOException, NotFoundException, ForbiddenException {
        String teamName = getTeamName(request);

        Session session = sessionsService.get();
        String email = request.getParameter(EMAIL_PARAM);
        Team team = teamsService.findTeam(teamName);

        teamsService.removeTeamMember(email, team, session);

        Audit.Event.TEAM_MEMBER_REMOVED
                .parameters()
                .host(request)
                .team(teamName)
                .teamMember(email)
                .actionedBy(session.getEmail())
                .log();
        return true;
    }

    /**
     * GET {@code /teams} returns the full json {@link Teams} details
     * <p>GET {@code /teams/[teamname]} returns the json {@link Team} details</p>
     *
     * @param request
     * @param response
     * @return {@link Teams} or {@link List<Team>} object
     * @throws IOException
     * @throws NotFoundException {@value org.eclipse.jetty.http.HttpStatus#NOT_FOUND_404} if the team doesn't exist
     */
    @GET
    public Object get(HttpServletRequest request, HttpServletResponse response) throws IOException, NotFoundException {
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            throw new NotFoundException("JWT sessions are enabled: GET /teams is no longer supported");
        }

        Object result = null;
        if (getTeamName(request) != null) {
            result = teamsService.findTeam(getTeamName(request));
        } else {
            List<Team> teams = teamsService.listTeams();
            teams.sort((t1, t2) -> t1.getName().toUpperCase().compareTo(t2.getName().toUpperCase()));
            result = new TeamList(teams);
        }
        return result;
    }


    private static String getTeamName(HttpServletRequest request) {

        Path path = Path.newInstance(request);
        List<String> segments = path.segments();

        if (segments.size() > 1) {
            return segments.get(1);
        }

        return null;
    }
}
