package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class GraphQLTeamByMemberSearchVariables extends GraphQLSearchVariables {
    public String userLogin;

    @JsonIgnore
    public GraphQLTeamByMemberSearchVariables next(GraphQLPageInfo pageInfo) {
        GraphQLTeamByMemberSearchVariables nextVariables = new GraphQLTeamByMemberSearchVariables();
        nextVariables.login = this.login;
        nextVariables.first = this.first;
        nextVariables.userLogin = this.userLogin;
        nextVariables.after = pageInfo.endCursor;

        return nextVariables;
    }
}