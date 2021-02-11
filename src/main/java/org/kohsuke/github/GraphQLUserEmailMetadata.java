package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphQLUserEmailMetadata {
    @JsonProperty("primary")
    public Boolean primary;

    @JsonProperty("type")
    public String type;

    @JsonProperty("value")
    public String value;
}
