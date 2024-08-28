package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SCIMMember {
    @JsonProperty("value")
    public String value;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("$ref")
    public String ref;
}