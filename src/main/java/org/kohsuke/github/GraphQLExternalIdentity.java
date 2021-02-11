package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of a GraphQL externalIdentity.
 *
 * @author Hiroyuki Wada
 */
public class GraphQLExternalIdentity extends GraphQLNode {

    @JsonProperty("guid")
    public String guid;

    @JsonProperty("organizationInvitation")
    public GraphQLOrganizationInvitation organizationInvitation;

    @JsonProperty("samlIdentity")
    public GraphQLExternalIdentitySamlAttributes samlIdentity;

    @JsonProperty("scimIdentity")
    public GraphQLExternalIdentityScimAttributes scimIdentity;

    @JsonProperty("user")
    public GraphQLUser user;

    @JsonIgnore
    public boolean isPending() {
        return organizationInvitation != null && user == null;
    }

    @JsonIgnore
    public boolean isCompleted() {
        return user != null && user.organization != null;
    }

    @JsonIgnore
    public boolean isDropped() {
        return user != null && user.organization == null;
    }

    @JsonIgnore
    public String getStatus() {
        if (isPending()) {
            return "pending";
        }
        if (isCompleted()) {
            return "active";
        }
        return "dropped";
    }
}
