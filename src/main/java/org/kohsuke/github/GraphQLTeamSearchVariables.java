package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class GraphQLTeamSearchVariables extends GraphQLSearchVariables {
    public String userLogin;

    @JsonIgnore
    public GraphQLTeamSearchVariables next(GraphQLPageInfo pageInfo) {
        GraphQLTeamSearchVariables nextVariables = new GraphQLTeamSearchVariables();
        nextVariables.login = this.login;
        nextVariables.first = this.first;
        nextVariables.userLogin = this.userLogin;
        nextVariables.after = pageInfo.endCursor;

        return nextVariables;
    }
}