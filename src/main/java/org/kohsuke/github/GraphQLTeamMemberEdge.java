package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of a GraphQL team member edge.
 *
 * @author Hiroyuki Wada
 */
public class GraphQLTeamMemberEdge extends GraphQLEdge {
    @JsonProperty("role")
    public GraphQLTeamMemberRole role;
}
