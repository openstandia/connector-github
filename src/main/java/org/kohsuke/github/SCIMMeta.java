package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SCIMMeta {
    @JsonProperty("created")
    public String created;

    @JsonProperty("lastModified")
    public String lastModified;
}
