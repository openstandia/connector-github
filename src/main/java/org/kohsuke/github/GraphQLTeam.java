package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of a GraphQL Team.
 *
 * @author Hiroyuki Wada
 */
public class GraphQLTeam extends GraphQLNode {
    @JsonProperty("databaseId")
    public Integer databaseId;

    @JsonProperty("slug")
    public String slug;

    @JsonProperty("members")
    public GraphQLTeamMemberConnection members;
}
