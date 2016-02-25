package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.Team;
import com.github.onsdigital.zebedee.json.TeamList;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.KeyManager;

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
 *
 * Endpoint that handles team membership
 *
 * <p>Calls are made to one of
 * <ul><li>{@code /teams}</li>
 * <li>{@code /teams/[teamname]}</li>
 * <li>{@code /teams/[teamname]?email=user@example.com}</li>
 * </ul></p>
 */
@Api
public class Teams {

    private static String getTeamName(HttpServletRequest request)
            throws IOException {

        Path path = Path.newInstance(request);
        List<String> segments = path.segments();

        if (segments.size() > 1) {
            return segments.get(1);
        }

        return null;
    }

    /**
     * POST {@code /teams/[teamname]} creates a team with name {@code teamname}
     * <p>POST {@code /teams/[teamname]?email=user@example.com} adds a user to the team</p>
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws ConflictException        {@value org.eclipse.jetty.http.HttpStatus#CONFLICT_409} if team already exists
     * @throws UnauthorizedException    {@value org.eclipse.jetty.http.HttpStatus#UNAUTHORIZED_401} if user does not have permission
     * @throws NotFoundException        {@value org.eclipse.jetty.http.HttpStatus#NOT_FOUND_404} if team can not be found/created
     */
    @POST
    public boolean post(HttpServletRequest request, HttpServletResponse response) throws IOException, ConflictException, UnauthorizedException, NotFoundException, BadRequestException {

        String email = request.getParameter("email");
        if (email == null) {
            return createTeam(request, response);
        } else {
            return addTeamMember(request, response);
        }
    }

    public boolean createTeam(HttpServletRequest request, HttpServletResponse response) throws IOException, ConflictException, UnauthorizedException, NotFoundException {

        Session session = Root.zebedee.sessions.get(request);
        String teamName = getTeamName(request);

        Root.zebedee.teams.createTeam(teamName, session);

        Audit.log(request, "Team %s created by %s", teamName, session.email);

        return true;
    }

    public boolean addTeamMember(HttpServletRequest request, HttpServletResponse response) throws UnauthorizedException, IOException, NotFoundException, BadRequestException {
        Zebedee zebedee = Root.zebedee;
        Session session = zebedee.sessions.get(request);

        String teamName = getTeamName(request);

        String email = request.getParameter("email");
        Team team = zebedee.teams.findTeam(teamName);

        Root.zebedee.teams.addTeamMember(email, team, session);
        evaluateCollectionKeys(zebedee, session, team, email);

        Audit.log(request, "Team %s member %s added by %s", teamName, email, session.email);

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
     * @throws UnauthorizedException    {@value org.eclipse.jetty.http.HttpStatus#UNAUTHORIZED_401} if user doesn't have delete permissions
     * @throws NotFoundException        {@value org.eclipse.jetty.http.HttpStatus#NOT_FOUND_404} if the team doesn't exist / user doesn't exist/ user isn't in this team
     * @throws BadRequestException      {@value org.eclipse.jetty.http.HttpStatus#CONFLICT_409} if the team cannot be deleted for some other reason
     */
    @DELETE
    public boolean delete(HttpServletRequest request, HttpServletResponse response) throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

        String email = request.getParameter("email");
        if (email == null) {
            return deleteTeam(request, response);
        } else {
            return removeTeamMember(request, response);
        }
    }

    public boolean deleteTeam(HttpServletRequest request, HttpServletResponse response) throws NotFoundException, BadRequestException, UnauthorizedException, IOException {

        String teamName = getTeamName(request);

        Zebedee zebedee = Root.zebedee;
        Session session = zebedee.sessions.get(request);
        Team team = zebedee.teams.findTeam(teamName);
        zebedee.teams.deleteTeam(team, session);

        evaluateCollectionKeys(zebedee, session, team, team.members.toArray(new String[team.members.size()]));

        Audit.log(request, "Team %s deleted by %s", teamName, session.email);

        return true;
    }

    public boolean removeTeamMember(HttpServletRequest request, HttpServletResponse response) throws UnauthorizedException, IOException, NotFoundException, BadRequestException {

        String teamName = getTeamName(request);

        Zebedee zebedee = Root.zebedee;
        Session session = Root.zebedee.sessions.get(request);
        String email = request.getParameter("email");
        Team team = zebedee.teams.findTeam(teamName);

        zebedee.teams.removeTeamMember(email, team, session);
        evaluateCollectionKeys(zebedee, session, team, email);

        Audit.log(request, "Team %s member %s removed by %s", teamName, email, session.email);

        return true;
    }

    /**
     * For a given list of user emails, evaluate if they should have keys added or removed.
     * @param zebedee
     * @param session
     * @param team
     * @param emails
     * @throws IOException
     * @throws NotFoundException
     * @throws BadRequestException
     */
    private void evaluateCollectionKeys(Zebedee zebedee, Session session, Team team, String... emails) throws IOException, NotFoundException, BadRequestException, UnauthorizedException {
        for (Collection collection : zebedee.collections.list()) {
            Set<Integer> teamIds = Root.zebedee.permissions.listViewerTeams(collection.description, session);
            if (teamIds != null && teamIds.contains(team.id)) {
                for (String memberEmail : emails) {
                    KeyManager.distributeKeyToUser(zebedee, collection, session, zebedee.users.get(memberEmail));
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
     * @throws NotFoundException        {@value org.eclipse.jetty.http.HttpStatus#NOT_FOUND_404} if the team doesn't exist
     */
    @GET
    public Object get(HttpServletRequest request, HttpServletResponse response) throws IOException, NotFoundException {
        Object result = null;
        if(getTeamName(request) != null) {
            result = Root.zebedee.teams.findTeam(getTeamName(request));
        } else {
            result = new TeamList(Root.zebedee.teams.listTeams());
        }
        return result;
    }
}
