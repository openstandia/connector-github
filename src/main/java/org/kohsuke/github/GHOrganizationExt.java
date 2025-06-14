package org.kohsuke.github;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extends original GHOrganization class.
 *
 * @author Hiroyuki Wada
 */
public class GHOrganizationExt extends GHOrganization {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    GHOrganizationExt wrapUp(GitHub root) {
        return (GHOrganizationExt) super.wrapUp(root);
    }

    public GHUser createInvitation(String email, String role) throws IOException {
        return root.createRequest()
                .method("POST")
                .withHeader("Accept", "application/vnd.github.v3+json")
                .with("email", email)
                .with("role", role)
                .withUrlPath(String.format("/orgs/%s/invitations", login))
                .fetch(GHUser.class);
    }

    public Iterable<GHUser> listInvitation() {
        return root.createRequest()
                .withUrlPath(String.format("/orgs/%s/invitations", login))
                .toIterable(GHUser[].class, item -> item.wrapUp(root));
    }

    public SCIMUser createSCIMUser(SCIMUser newUser) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", newUser.userName);
        map.put("emails", newUser.emails);
        map.put("name", newUser.name);
        map.put("externalId", newUser.externalId);

        SCIMUser u = root.createRequest()
                .method("POST")
                .withHeader(SCIMConstants.HEADER_ACCEPT, SCIMConstants.SCIM_ACCEPT)
                .withHeader(SCIMConstants.GITHUB_API_VERSION, SCIMConstants.GITHUB_API_VERSION)
                .with(map)
                .withUrlPath(String.format("/scim/v2/organizations/%s/Users", login))
                .fetch(SCIMUser.class);
        return u;
    }

    public SCIMUser updateSCIMUser(String scimUserId, String scimUserName, String scimEmail, String scimGivenName, String scimFamilyName) throws IOException {
        List<SCIMOperation> ops = new ArrayList<>();

        if (scimUserName != null) {
            ops.add(new SCIMOperation<>("replace", "userName", scimUserName));
        }
        if (scimEmail != null) {
            List<Map<String, String>> emails = new ArrayList<>();
            Map<String, String> emailsMap = new HashMap<>();
            emailsMap.put("value", scimEmail);
            emails.add(emailsMap);
            ops.add(new SCIMOperation<>("replace", "emails", emails));
        }
        if (scimGivenName != null) {
            ops.add(new SCIMOperation<>("replace", "name.givenName", scimGivenName));
        }
        if (scimFamilyName != null) {
            ops.add(new SCIMOperation<>("replace", "name.familyName", scimFamilyName));
        }

        if (ops.isEmpty()) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("Operations", ops);

        SCIMUser u = root.createRequest()
                .method("PATCH")
                .withHeader(SCIMConstants.HEADER_ACCEPT, SCIMConstants.SCIM_ACCEPT)
                .withHeader(SCIMConstants.GITHUB_API_VERSION, SCIMConstants.GITHUB_API_VERSION)
                .with(map)
                .withUrlPath(String.format("/scim/v2/organizations/%s/Users/%s", login, scimUserId))
                .fetch(SCIMUser.class);
        return u;
    }

    public SCIMUser getSCIMUser(String scimUserId) throws IOException {
        SCIMUser u = root.createRequest()
                .withHeader(SCIMConstants.HEADER_ACCEPT, SCIMConstants.SCIM_ACCEPT)
                .withHeader(SCIMConstants.GITHUB_API_VERSION, SCIMConstants.GITHUB_API_VERSION)
                .withUrlPath(String.format("/scim/v2/organizations/%s/Users/%s", login, scimUserId))
                .fetch(SCIMUser.class);
        return u;
    }

    public SCIMUser getSCIMUserByUserName(String scimUserName) throws IOException {
        SCIMUser u = root.createRequest()
                .withHeader(SCIMConstants.HEADER_ACCEPT, SCIMConstants.SCIM_ACCEPT)
                .withHeader(SCIMConstants.GITHUB_API_VERSION, SCIMConstants.GITHUB_API_VERSION)
                .withUrlPath(String.format("/scim/v2/organizations/%s/Users?filter=userName eq \"%s\"", login, scimUserName))
                .fetch(SCIMUser.class);
        return u;
    }

    /**
     * Search users.
     *
     * @return the gh user search builder
     */
    public SCIMUserSearchBuilder searchSCIMUsers() {
        return new SCIMUserSearchBuilder(root, this);
    }

    public SCIMPagedSearchIterable<SCIMUser> listSCIMUsers(int pageSize)
            throws IOException {
        return searchSCIMUsers().list().withPageSize(pageSize);
    }

    /**
     * Search users.
     *
     * @return the gh user search builder
     */
    public GraphQLOrganizationExternalIdentitySearchBuilder searchExternalIdentities() {
        return new GraphQLOrganizationExternalIdentitySearchBuilder(root, this);
    }

    public GraphQLPagedSearchIterable<GraphQLOrganization, GraphQLExternalIdentityEdge> listExternalIdentities(int pageSize)
            throws IOException {
        return searchExternalIdentities().list().withPageSize(pageSize);
    }

    public void deleteSCIMUser(String scimUserId) throws IOException {
        root.createRequest()
                .method("DELETE")
                .withHeader(SCIMConstants.HEADER_ACCEPT, SCIMConstants.SCIM_ACCEPT)
                .withHeader(SCIMConstants.GITHUB_API_VERSION, SCIMConstants.GITHUB_API_VERSION)
                .withUrlPath(String.format("/scim/v2/organizations/%s/Users/%s", login, scimUserId))
                .send();
    }

    public GHTeamExt getTeam(long teamId) throws IOException {
        return root.createRequest()
                .withUrlPath(String.format("/organizations/%d/team/%d", getId(), teamId))
                .fetch(GHTeamExt.class);
    }

    public GHTeamExt getTeam(String slug) throws IOException {
        return root.createRequest()
                .withUrlPath(String.format("/organizations/%d/team/%d", login, slug))
                .fetch(GHTeamExt.class);
    }

    public PagedIterable<GHTeamExt> listTeamsExt() throws IOException {
        return root.createRequest()
                .withUrlPath(String.format("/orgs/%s/teams", login))
                .toIterable(GHTeamExt[].class, item -> item.wrapUp(this));
    }

    public GHTeam updateTeam(long teamId, String name, String description, GHTeam.Privacy privacy, Long parentTeamId,
                             boolean clearParent) throws IOException {
        Requester req = root.createRequest().method("PATCH");

        if (name != null) {
            req.with("name", name);
        }
        if (description != null) {
            req.with("description", description);
        }
        if (privacy != null) {
            req.with("privacy", privacy);
        }
        if (parentTeamId != null) {
            req.with("parent_team_id", parentTeamId);
        } else if (clearParent) {
            req.withNullable("parent_team_id", null);
        }

        GHTeam updated = req.withUrlPath(String.format("/organizations/%d/team/%d", getId(), teamId))
                .fetch(GHTeam.class);

        return updated;
    }

    public void deleteTeam(long teamId) throws IOException {
        root.createRequest().method("DELETE")
                .withUrlPath(String.format("/organizations/%d/team/%d", getId(), teamId))
                .send();
    }

    public GraphQLPagedSearchIterable<GraphQLOrganization, GraphQLTeamEdge> findTeam(String teamName, int pageSize) throws IOException {
        return new GraphQLTeamSearchBuilder(root, this, teamName)
                .list()
                .withPageSize(pageSize);
    }

    public GraphQLTeamByMemberSearchBuilder searchTeams(String userLogin) {
        return new GraphQLTeamByMemberSearchBuilder(root, this, userLogin);
    }

    public GraphQLPagedSearchIterable<GraphQLOrganization, GraphQLTeamEdge> listTeams(String userLogin, int pageSize)
            throws IOException {
        return searchTeams(userLogin).list().withPageSize(pageSize);
    }

    public void addTeamMembership(long teamId, String userLogin, GHTeam.Role teamRole) throws IOException {
        root.createRequest().method("PUT")
                .with("role", teamRole.name().toLowerCase())
                .withUrlPath(String.format("/organizations/%d/team/%d/memberships/%s", getId(), teamId, userLogin))
                .send();
    }

    public void removeTeamMembership(long teamId, String userLogin) throws IOException {
        root.createRequest().method("DELETE")
                .withUrlPath(String.format("/organizations/%d/team/%d/memberships/%s", getId(), teamId, userLogin))
                .send();
    }

    public boolean isMember(String userLogin) {
        try {
            root.createRequest()
                    .withUrlPath(String.format("/orgs/%s/members/%s", login, userLogin))
                    .send();
            return true;
        } catch (IOException ignore) {
            return false;
        }
    }

    /**
     * Set organization role to the user.
     * https://docs.github.com/en/rest/reference/orgs#set-organization-membership-for-a-user
     *
     * @param userLogin        GitHub username
     * @param organizationRole orgnization role (admin or member)
     * @throws IOException API error
     */
    public void setOrganizationMembership(String userLogin, Role organizationRole) throws IOException {
        root.createRequest().method("PUT")
                .with("role", organizationRole.name().toLowerCase())
                .withUrlPath(String.format("/orgs/%s/memberships/%s", login, userLogin))
                .send();
    }

    public GHMembership getOrganizationMembership(String userLogin) throws IOException {
        return root.createRequest()
                .withUrlPath(String.format("/orgs/%s/memberships/%s", login, userLogin))
                .fetch(GHMembership.class);
    }
}