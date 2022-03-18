package com.github.onsdigital.zebedee.teams.service;

import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Set;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;

/**
 * Implementation of the TeamsService to be used during migration to JWT login using the dp-identity-api. All methods
 * will throw exceptions to help in identifying any missing dependencies we forget to update.
 */
public class StubbedTeamsServiceImpl implements TeamsService {

    private static final String UNSUPPORTED_METHOD = "unsupported attempt to call pre-JWT team service when JWT sessions are enabled";

    /**
     * @deprecated as Florence can request the team names from the dp-identity-api for presentation on the front end
     */
    @Deprecated
    @Override
    public List<Team> resolveTeamDetails(Set<String> teamIds) {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated as teams management is moving to the dp-identity-api and the references to this method that will still
     *             be required after migration (i.e. those in the current {@link com.github.onsdigital.zebedee.permissions.service.PermissionsService}
     *             implementation can be reworked to use the groups list within the JWT session rather than needing to
     *             implement calls to the dp-identity-api.
     */
    @Deprecated
    @Override
    public List<Team> listTeams() {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated as teams management is moving to the dp-identity-api and the references to this method will be
     *             removed.
     */
    @Deprecated
    @Override
    public List<Team> resolveTeams(Set<String> teamIds) {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated because this would be ugly to implement in the old world, and it is basically tech debt anyways since
     *             it is used when the collection endpoints accept the `teams` array as a list of team names rather than
     *             a list of team IDs as the comment on the original field in {@link com.github.onsdigital.zebedee.json.CollectionBase}
     *             describes. After the migration the model will be returned to the original state of passing the ids
     *             rather than the names and this method will be removed.
     */
    @Deprecated
    @Override
    public Team findTeam(String teamName) {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated because teams management will be moving to the dp-identity-api. Once the migration to the new service
     *             is completed this method will be removed.
     */
    @Deprecated
    @Override
    public Team createTeam(String teamName, Session session) {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated because teams management will be moving to the dp-identity-api. Once the migration to the new service
     *             is completed this method will be removed.
     */
    @Deprecated
    @Override
    public void deleteTeam(Team delete, Session session) {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated because teams management will be moving to the dp-identity-api. Once the migration to the new service
     *             is completed this method will be removed.
     */
    @Deprecated
    @Override
    public void addTeamMember(String email, Team team, Session session) {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated because teams management will be moving to the dp-identity-api. Once the migration to the new service
     *             is completed this method will be removed.
     */
    @Deprecated
    @Override
    public void removeTeamMember(String email, Team team, Session session) {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated because teams management will be moving to the dp-identity-api. Once the migration to the new service
     *             is completed this method will be removed.
     */
    @Deprecated
    @Override
    public List<AbstractMap.SimpleEntry<String, String>> getTeamMembersSummary(Session session) {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated because teams management will be moving to the dp-identity-api. Once the migration to the new service
     *             is completed this method will be removed.
     */
    @Deprecated
    @Override
    public List<String> listTeamsForUser(Session session) throws IOException {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }
}