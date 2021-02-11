package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SCIMUser {
    @JsonProperty("id")
    public String id;

    @JsonProperty("userName")
    public String userName;

    @JsonProperty("name")
    public SCIMName name;

    @JsonProperty("emails")
    public SCIMEmail[] emails;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("externalId")
    public String externalId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("active")
    public Boolean active;
}