package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.*;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.teams.model.TeamList;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.KeyManager;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.user.service.UsersService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by thomasridd on 28/04/15.
 * <p>
 * Endpoint that handles team membership
 * <p>
 * <p>Calls are made to one of
 * <ul><li>{@code /teams}</li>
 * <li>{@code /teams/[teamname]}</li>
 * <li>{@code /teams/[teamname]?email=user@example.com}</li>
 * </ul></p>
 */
@Api
public class Teams {

    /**
     * Wrap static method calls to obtain service in function makes testing easier - class member can be
     * replaced with a mocked giving control of desired behaviour.
     */
    private ServiceSupplier<UsersService> usersServiceSupplier = () -> Root.zebedee.getUsersService();


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
    public boolean post(HttpServletRequest request, HttpServletResponse response) throws IOException, ConflictException, UnauthorizedException, NotFoundException, BadRequestException, ForbiddenException {

        String email = request.getParameter("email");
        if (email == null) {
            return createTeam(request, response);
        } else {
            return addTeamMember(request, response);
        }
    }

    public boolean createTeam(HttpServletRequest request, HttpServletResponse response) throws IOException, ConflictException, UnauthorizedException, NotFoundException, ForbiddenException {

        Session session = Root.zebedee.getSessionsService().get(request);
        String teamName = getTeamName(request);

        Root.zebedee.getTeamsService().createTeam(teamName, session);

        Audit.Event.TEAM_CREATED
                .parameters()
                .host(request)
                .team(teamName)
                .actionedBy(session.getEmail())
                .log();
        return true;
    }

    public boolean addTeamMember(HttpServletRequest request, HttpServletResponse response) throws UnauthorizedException, IOException, NotFoundException, BadRequestException, ForbiddenException {
        Zebedee zebedee = Root.zebedee;
        Session session = zebedee.getSessionsService().get(request);

        String teamName = getTeamName(request);

        String email = request.getParameter("email");
        Team team = zebedee.getTeamsService().findTeam(teamName);

        Root.zebedee.getTeamsService().addTeamMember(email, team, session);
        evaluateCollectionKeys(zebedee, session, team, email);

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
    public boolean delete(HttpServletRequest request, HttpServletResponse response) throws IOException, UnauthorizedException, NotFoundException, BadRequestException, ForbiddenException {

        String email = request.getParameter("email");
        if (email == null) {
            return deleteTeam(request, response);
        } else {
            return removeTeamMember(request, response);
        }
    }

    public boolean deleteTeam(HttpServletRequest request, HttpServletResponse response) throws NotFoundException, BadRequestException, UnauthorizedException, IOException, ForbiddenException {

        String teamName = getTeamName(request);

        Zebedee zebedee = Root.zebedee;
        Session session = zebedee.getSessionsService().get(request);
        Team team = zebedee.getTeamsService().findTeam(teamName);
        zebedee.getTeamsService().deleteTeam(team, session);

        evaluateCollectionKeys(zebedee, session, team, team.getMembers().toArray(new String[team.getMembers().size()]));

        Audit.Event.TEAM_DELETED
                .parameters()
                .host(request)
                .team(teamName)
                .actionedBy(session.getEmail())
                .log();
        return true;
    }

    public boolean removeTeamMember(HttpServletRequest request, HttpServletResponse response) throws UnauthorizedException, IOException, NotFoundException, BadRequestException, ForbiddenException {

        String teamName = getTeamName(request);

        Zebedee zebedee = Root.zebedee;
        Session session = Root.zebedee.getSessionsService().get(request);
        String email = request.getParameter("email");
        Team team = zebedee.getTeamsService().findTeam(teamName);

        zebedee.getTeamsService().removeTeamMember(email, team, session);
        evaluateCollectionKeys(zebedee, session, team, email);

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
     * For a given list of user emails, evaluate if they should have keys added or removed.
     *
     * @param zebedee
     * @param session
     * @param team
     * @param emails
     * @throws IOException
     * @throws NotFoundException
     * @throws BadRequestException
     */
    private void evaluateCollectionKeys(Zebedee zebedee, Session session, Team team, String... emails) throws IOException, NotFoundException, BadRequestException, UnauthorizedException {
        for (Collection collection : zebedee.getCollections().list()) {
            Set<Integer> teamIds = Root.zebedee.getPermissionsService().listViewerTeams(collection.description, session);
            if (teamIds != null && teamIds.contains(team.getId())) {
                for (String memberEmail : emails) {
                    KeyManager.distributeKeyToUser(zebedee, collection, session,
                            usersServiceSupplier.getService().getUserByEmail(memberEmail));
                }
            }
        }
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
        Object result = null;
        if (getTeamName(request) != null) {
            result = Root.zebedee.getTeamsService().findTeam(getTeamName(request));
        } else {
            List<Team> teams = Root.zebedee.getTeamsService().listTeams();
            teams.sort((t1, t2) -> t1.getName().toUpperCase().compareTo(t2.getName().toUpperCase()));
            result = new TeamList(teams);
        }
        return result;
    }


    private static String getTeamName(HttpServletRequest request)
            throws IOException {

        Path path = Path.newInstance(request);
        List<String> segments = path.segments();

        if (segments.size() > 1) {
            return segments.get(1);
        }

        return null;
    }
}
