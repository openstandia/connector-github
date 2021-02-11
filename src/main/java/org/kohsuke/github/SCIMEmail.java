package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SCIMEmail {
    @JsonProperty("value")
    public String value;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("primary")
    public Boolean primary;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("type")
    public String type;
}