package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class GraphQLSearchVariables {
    public String login;
    public int first;
    public String after;

    @JsonIgnore
    public GraphQLSearchVariables next(GraphQLPageInfo pageInfo) {
        GraphQLSearchVariables nextVariables = new GraphQLSearchVariables();
        nextVariables.login = this.login;
        nextVariables.first = this.first;
        nextVariables.after = pageInfo.endCursor;

        return nextVariables;
    }
}