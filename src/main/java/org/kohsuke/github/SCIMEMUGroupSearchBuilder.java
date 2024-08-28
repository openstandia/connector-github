package org.kohsuke.github;

/**
 * Search SCIM users.
 *
 * @author Hiroyuki Wada
 */
public class SCIMEMUGroupSearchBuilder extends SCIMSearchBuilder<SCIMEMUGroup> {

    SCIMEMUGroupSearchBuilder(GitHub root, GHEnterpriseExt enterprise) {
        super(root, enterprise, SCIMUserSearchResult.class);
    }

    private static class SCIMUserSearchResult extends SCIMSearchResult<SCIMEMUGroup> {
    }

    @Override
    protected String getApiUrl() {
        return String.format("/scim/v2/enterprises/%s/Groups", enterprise.login);
    }
}
