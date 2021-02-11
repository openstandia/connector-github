package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphQLUser extends GraphQLNode {
    @JsonProperty("login")
    public String login;

    @JsonProperty("databaseId")
    public int databaseId;

    @JsonProperty("organization")
    public GraphQLOrganization organization;
}
