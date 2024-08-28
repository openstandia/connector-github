package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SCIMEMUGroup {
    @JsonProperty("meta")
    public SCIMMeta meta;

    @JsonProperty("id")
    public String id;

    @JsonProperty("displayName")
    public String displayName;

    @JsonProperty("members")
    public List<SCIMMember> members;

    @JsonProperty("externalId")
    public String externalId;
}