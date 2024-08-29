package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SCIMRole {
    @JsonProperty("value")
    public String value;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("primary")
    public Boolean primary;
}