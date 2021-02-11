package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphQLOrganizationInvitation extends GraphQLNode {
    @JsonProperty("email")
    public String email;
}
