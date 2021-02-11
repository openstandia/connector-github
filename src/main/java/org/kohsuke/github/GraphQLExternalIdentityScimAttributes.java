package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphQLExternalIdentityScimAttributes {
    @JsonProperty("emails")
    public GraphQLUserEmailMetadata[] emails;

    @JsonProperty("username")
    public String username;

    @JsonProperty("givenName")
    public String givenName;

    @JsonProperty("familyName")
    public String familyName;
}
