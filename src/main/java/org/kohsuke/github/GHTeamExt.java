package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GHTeamExt extends GHTeam {
    private GHOrganization organization;

    GHTeamExt wrapUp(GHOrganization owner) {
        this.organization = owner;
        this.root = owner.root;
        return this;
    }

    @JsonProperty("parent")
    private GHTeam parent;

    public GHTeam getParent() {
        return parent;
    }
}
