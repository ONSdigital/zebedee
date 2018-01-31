package com.github.onsdigital.zebedee.teams.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.ForbiddenException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.teams.store.TeamsStore;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.teams.model.Team.teamIDComparator;

/**
 * Handles permissions mapping between users and {@link com.github.onsdigital.zebedee.Zebedee} functions.
 * Created by david on 12/03/2015.
 */
public class TeamsServiceImpl implements TeamsService {

    private static final String FORBIDDEN_ERR_MSG = "User does not have the required admin permission to perform " +
            "requested action.";

    private static final int DEFAULT_TEAM_ID = 1;

    private ReadWriteLock teamLock = new ReentrantReadWriteLock();
    private ServiceSupplier<PermissionsService> permissionsServiceSupplier;
    private TeamsStore teamsStore;

    private Comparator<Team> sortTeamsbyName = (t1, t2) -> t1.getName().compareTo(t2.getName());

    private Comparator<AbstractMap.SimpleEntry<String, String>> sortByTeamName =
            (o1, o2) -> o1.getKey().compareTo(o2.getKey());

    private Comparator<AbstractMap.SimpleEntry<String, String>> sortTeamByMemberName =
            (o1, o2) -> o1.getValue().compareTo(o2.getValue());

    private Comparator<AbstractMap.SimpleEntry<String, String>> sortTeamsReportList =
            sortByTeamName.thenComparing(sortTeamByMemberName);

    /**
     * @param teamsStore
     * @param permissionsServiceSupplier
     */
    public TeamsServiceImpl(TeamsStore teamsStore, ServiceSupplier<PermissionsService> permissionsServiceSupplier) {
        this.teamsStore = teamsStore;
        this.permissionsServiceSupplier = permissionsServiceSupplier;
    }

    @Override
    public List<Team> listTeams() throws IOException {
        return teamsStore.listTeams();
    }

    @Override
    public List<Team> resolveTeams(Set<Integer> teamIds) throws IOException {
        return listTeams()
                .parallelStream()
                .filter(t -> teamIds.contains(t.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Team> resolveTeamDetails(Set<Integer> teamIds) throws IOException {
        return resolveTeams(teamIds).stream()
                .map(team -> new Team().setId(team.getId()).setName(team.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public Team findTeam(String teamName) throws IOException, NotFoundException {
        return teamsStore.get(teamName);
    }

    @Override
    public Team createTeam(String teamName, Session session) throws IOException, UnauthorizedException, ConflictException, NotFoundException, ForbiddenException {
        validateSessionAndPermissions(session);

        // Check for a name conflict:
        if (teamsStore.exists(teamName)) {
            throw new ConflictException("There is already a team matching this name.");
        }

        teamLock.writeLock().lock();
        try {
            // Order existing teams by their ID.
            List<Team> orderedTeams = listTeams()
                    .parallelStream()
                    .sorted(teamIDComparator)
                    .collect(Collectors.toList());

            // Reverse the order so the highest number is first (for convenience).
            Collections.reverse(orderedTeams);

            // For the record this is a lift & shift of the pervious implementation (albeit a bit refactored).
            // When we move to a database impl this can be properly.
            int teamID = orderedTeams.isEmpty() ? DEFAULT_TEAM_ID : (1 + orderedTeams.get(0).getId());

            Team team = new Team()
                    .setName(teamName)
                    .setId(teamID);
            teamsStore.save(team);
            return team;
        } finally {
            teamLock.writeLock().unlock();
        }
    }

    @Override
    public void deleteTeam(Team delete, Session session) throws IOException, UnauthorizedException, NotFoundException, BadRequestException, ForbiddenException {
        validateSessionAndPermissions(session);
        if (!teamsStore.deleteTeam(delete)) {
            logDebug("Team could not be deleted").addParameter("teamName", delete.getName()).log();
            throw new IOException("Team " + delete.getName() + " could not be deleted.");
        }
    }

    @Override
    public void addTeamMember(String email, Team team, Session session) throws IOException, UnauthorizedException, NotFoundException, ForbiddenException {
        validateSessionAndPermissions(session);
        updateTeam(
                team,
                (t) -> !StringUtils.isBlank(email) && team != null,
                (t) -> t.addMember(email));
    }

    @Override
    public void removeTeamMember(String email, Team team, Session session) throws IOException, UnauthorizedException, NotFoundException, ForbiddenException {
        validateSessionAndPermissions(session);
        updateTeam(
                team,
                (t) -> !StringUtils.isBlank(email) && team != null,
                (t) -> t.removeMember(email));
    }

    @Override
    public List<AbstractMap.SimpleEntry<String, String>> getTeamMembersSummary(Session session) throws IOException, UnauthorizedException, ForbiddenException {
        validateSessionAndPermissions(session);
        List<AbstractMap.SimpleEntry<String, String>> membersReport = new ArrayList<>();

        for (Team team : listTeams()) {
            if (team.getMembers() == null || team.getMembers().isEmpty()) {
                membersReport.add(new AbstractMap.SimpleEntry<String, String>(team.getName(), ""));
                continue;
            }

            membersReport.addAll(team.getMembers()
                    .stream()
                    .map(user -> new AbstractMap.SimpleEntry<String, String>(team.getName(), user))
                    .collect(Collectors.toSet()));
        }

        Collections.sort(membersReport, sortTeamsReportList);
        return membersReport;
    }

    private void updateTeam(Team target, Predicate<Team> validator, Function<Team, Team> updateTask) throws IOException, NotFoundException {
        if (validator.test(target)) {
            teamLock.writeLock().lock();
            try {
                teamsStore.save(updateTask.apply(target));
            } finally {
                teamLock.writeLock().unlock();
            }
        }
    }

    /**
     * Check the {@link Session} is not null & the email address is not empty and that the user have the required
     * admin permission.
     *
     * @param session the {@link Session} to validate.
     * @throws UnauthorizedException if the session is null, the session email is empty or the user does not have the
     *                               admin permission.
     * @throws IOException           problem checking the permission.
     */
    private void validateSessionAndPermissions(Session session) throws UnauthorizedException, IOException, ForbiddenException {
        if (session == null || StringUtils.isEmpty(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }
        if (!permissionsServiceSupplier.getService().isAdministrator(session.getEmail())) {
            logInfo(FORBIDDEN_ERR_MSG).log();
            throw new ForbiddenException(FORBIDDEN_ERR_MSG);
        }
    }

}
