package org.kohsuke.github;

/**
 * Search SCIM users.
 *
 * @author Hiroyuki Wada
 */
public class SCIMEMUUserSearchBuilder extends SCIMSearchBuilder<SCIMEMUUser> {

    SCIMEMUUserSearchBuilder(GitHub root, GHEnterpriseExt enterprise) {
        super(root, enterprise, SCIMUserSearchResult.class);
    }

    private static class SCIMUserSearchResult extends SCIMSearchResult<SCIMEMUUser> {
    }

    @Override
    protected String getApiUrl() {
        return String.format("/scim/v2/enterprises/%s/Users", enterprise.login);
    }
}
