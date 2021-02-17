package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of a GraphQL Team.
 *
 * @author Hiroyuki Wada
 */
public class GraphQLTeam extends GraphQLNode {
    @JsonProperty("name")
    public String name;

    @JsonProperty("databaseId")
    public Integer databaseId;

    @JsonProperty("slug")
    public String slug;

    @JsonProperty("description")
    public String description;

    @JsonProperty("members")
    public GraphQLTeamMemberConnection members;

    @JsonProperty("privacy")
    public GraphQLTeamPrivacy privacy;

    @JsonProperty("parentTeam")
    public GraphQLTeam parentTeam;
}
