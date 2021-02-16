package org.kohsuke.github;

import java.util.Map;
import java.util.function.Function;

/**
 * Search organization external identities by GitHub GraphQL API.
 *
 * @author Hiroyuki Wada
 */
public class GraphQLOrganizationExternalIdentitySearchBuilder extends GraphQLSearchBuilder<GraphQLOrganization, GraphQLExternalIdentityEdge, GraphQLSearchVariables> {
    private final String query = "query($login: String!, $first: Int!, $after: String) {\n" +
            "  organization(login: $login) {\n" +
            "    id\n" +
            "    login\n" +
            "    databaseId\n" +
            "    samlIdentityProvider {\n" +
            "      externalIdentities(first: $first, after: $after) {\n" +
            "        totalCount\n" +
            "        pageInfo {\n" +
            "          endCursor\n" +
            "          hasNextPage\n" +
            "          hasPreviousPage\n" +
            "          startCursor\n" +
            "        }\n" +
            "        edges {\n" +
            "          cursor\n" +
            "          node {\n" +
            "            id\n" +
            "            guid\n" +
            "            organizationInvitation {\n" +
            "              id\n" +
            "              email\n" +
            "            }\n" +
            "            user {\n" +
            "              id\n" +
            "              login\n" +
            "              databaseId\n" +
            "              organization(login: $login) {\n" +
            "                id\n" +
            "                login\n" +
            "                databaseId\n" +
            "              }\n" +
            "            }\n" +
            "            scimIdentity {\n" +
            "              username\n" +
            "              emails {\n" +
            "                value\n" +
            "                primary\n" +
            "              }\n" +
            "              givenName\n" +
            "              familyName\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    GraphQLOrganizationExternalIdentitySearchBuilder(GitHub root, GHOrganization org) {
        super(root, org, GraphQLOrganizationSearchResult.class);
        this.variables.login = org.login;
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
    protected Function<GraphQLSearchResult<GraphQLOrganization>, GraphQLPageInfo> getPageInfo() {
        return (result) -> result.getData().samlIdentityProvider.externalIdentities.pageInfo;
    }

    @Override
    protected Function<GraphQLSearchResult<GraphQLOrganization>, GraphQLExternalIdentityEdge[]> getEdges() {
        return (result) -> result.getData().samlIdentityProvider.externalIdentities.edges;
    }
}
