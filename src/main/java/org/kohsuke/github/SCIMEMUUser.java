package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SCIMEMUUser {
    @JsonProperty("meta")
    public SCIMMeta meta;

    @JsonProperty("id")
    public String id;

    @JsonProperty("userName")
    public String userName;

    @JsonProperty("name")
    public SCIMName name;

    @JsonProperty("emails")
    public List<SCIMEmail> emails;

    @JsonProperty("externalId")
    public String externalId;

    @JsonProperty("active")
    public Boolean active;

    @JsonProperty("displayName")
    public String displayName;

    @JsonProperty("roles")
    public List<SCIMRole> roles;

    // Read only
    @JsonProperty("groups")
    public List<SCIMMember> groups;
}