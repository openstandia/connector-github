package org.kohsuke.github;

import java.util.Map;
import java.util.function.Function;

/**
 * Search organization's teams by member's login name with GitHub GraphQL API.
 *
 * @author Hiroyuki Wada
 */
public class GraphQLTeamByMemberSearchBuilder extends GraphQLSearchBuilder<GraphQLOrganization, GraphQLTeamEdge, GraphQLTeamByMemberSearchVariables> {
    private final String query = "query($login: String!, $userLogin: String!, $first: Int!, $after: String) {\n" +
            "  organization(login: $login) {\n" +
            "    id\n" +
            "    login\n" +
            "    databaseId\n" +
            "    teams(userLogins: [$userLogin], first: $first, after: $after) {\n" +
            "      totalCount\n" +
            "      pageInfo {\n" +
            "        endCursor\n" +
            "        hasNextPage\n" +
            "        hasPreviousPage\n" +
            "        startCursor\n" +
            "      }\n" +
            "      edges {\n" +
            "        cursor\n" +
            "        node {\n" +
            "          id\n" +
            "          databaseId\n" +
            "          slug\n" +
            "          members(query: $userLogin) {\n" +
            "            totalCount\n" +
            "            edges {\n" +
            "              role\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    GraphQLTeamByMemberSearchBuilder(GitHub root, GHOrganization org, String userLogin) {
        super(root, org, GraphQLOrganizationSearchResult.class);
        this.variables.login = org.login;
        this.variables.userLogin = userLogin;
    }

    private static class GraphQLOrganizationSearchResult extends GraphQLSearchResult<GraphQLOrganization> {
        public GraphQLOrganization organization;

        @Override
        public void setData(Map<String, GraphQLOrganization> data) {
            this.organization = data.get("organization");
        }

        @Override
        public GraphQLOrganization getData() {
            return organization;
        }
    }

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    protected GraphQLTeamByMemberSearchVariables initSearchVariables() {
        return new GraphQLTeamByMemberSearchVariables();
    }

    @Override
    protected Function<GraphQLSearchResult<GraphQLOrganization>, GraphQLPageInfo> getPageInfo() {
        return (result) -> result.getData().teams.pageInfo;
    }

    @Override
    protected Function<GraphQLSearchResult<GraphQLOrganization>, GraphQLTeamEdge[]> getEdges() {
        return (result) -> result.getData().teams.edges;
    }
}
