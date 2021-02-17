package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class GraphQLTeamSearchVariables extends GraphQLSearchVariables {
    public String teamName;

    @JsonIgnore
    public GraphQLTeamSearchVariables next(GraphQLPageInfo pageInfo) {
        GraphQLTeamSearchVariables nextVariables = new GraphQLTeamSearchVariables();
        nextVariables.login = this.login;
        nextVariables.first = this.first;
        nextVariables.teamName = this.teamName;
        nextVariables.after = pageInfo.endCursor;

        return nextVariables;
    }
}