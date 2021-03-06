package org.kohsuke.github;

import java.util.Map;
import java.util.function.Function;

/**
 * Search organization's teams by name with GitHub GraphQL API.
 *
 * @author Hiroyuki Wada
 */
public class GraphQLTeamSearchBuilder extends GraphQLSearchBuilder<GraphQLOrganization, GraphQLTeamEdge, GraphQLTeamSearchVariables> {
    private final String query = "query($login: String!, $teamName: String!, $first: Int!, $after: String) {\n" +
            "  organization(login: $login) {\n" +
            "    id\n" +
            "    login\n" +
            "    databaseId\n" +
            "    teams(query: $teamName, first: $first, after: $after) {\n" +
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
            "          name\n" +
            "          slug\n" +
            "          description\n" +
            "          privacy\n" +
            "          parentTeam {\n" +
            "            id\n" +
            "            databaseId\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    GraphQLTeamSearchBuilder(GitHub root, GHOrganization org, String teamName) {
        super(root, org, GraphQLOrganizationSearchResult.class);
        this.variables.login = org.login;
        this.variables.teamName = teamName;
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
    protected GraphQLTeamSearchVariables initSearchVariables() {
        return new GraphQLTeamSearchVariables();
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
