package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SCIMName {
    @JsonProperty("givenName")
    public String givenName;

    @JsonProperty("familyName")
    public String familyName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("formatted")
    public String formatted;
}
