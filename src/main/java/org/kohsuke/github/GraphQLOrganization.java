package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of a GraphQL Organization.
 *
 * @author Hiroyuki Wada
 */
public class GraphQLOrganization extends GraphQLNode {
    @JsonProperty("login")
    public String login;

    @JsonProperty("databaseId")
    public String databaseId;

    @JsonProperty("samlIdentityProvider")
    public GraphQLOrganizationIdentityProvider samlIdentityProvider;

    @JsonProperty("teams")
    public GraphQLTeamConnection teams;
}
