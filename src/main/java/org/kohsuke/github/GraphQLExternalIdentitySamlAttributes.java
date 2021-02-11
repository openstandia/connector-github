package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphQLExternalIdentitySamlAttributes {
    @JsonProperty("nameId")
    public String nameId;

    @JsonProperty("username")
    public String username;

    @JsonProperty("emails")
    public GraphQLUserEmailMetadata[] emails;
}
