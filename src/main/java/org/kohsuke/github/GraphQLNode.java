package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphQLNode {
    @JsonProperty("id")
    public String id;
}
