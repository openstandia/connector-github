package org.kohsuke.github;

/**
 * Search SCIM users.
 *
 * @author Hiroyuki Wada
 */
public class SCIMUserSearchBuilder extends SCIMSearchBuilder<SCIMUser> {

    SCIMUserSearchBuilder(GitHub root, GHOrganization org) {
        super(root, org, SCIMUserSearchResult.class);
    }

    private static class SCIMUserSearchResult extends SCIMSearchResult<SCIMUser> {
    }

    @Override
    protected String getApiUrl() {
        return String.format("/scim/v2/organizations/%s/Users", organization.login);
    }
}
