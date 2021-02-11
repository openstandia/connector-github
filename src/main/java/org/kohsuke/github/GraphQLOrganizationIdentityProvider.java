package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of a GraphQL OrganizationIdentityProvider.
 *
 * @author Hiroyuki Wada
 */
public class GraphQLOrganizationIdentityProvider extends GraphQLNode {
    @JsonProperty("externalIdentities")
    public GraphQLExternalIdentityConnection externalIdentities;
}
