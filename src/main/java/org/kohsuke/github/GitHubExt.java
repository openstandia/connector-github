package org.kohsuke.github;

import org.kohsuke.github.authorization.AuthorizationProvider;

import java.io.IOException;

/**
 * Extends original GitHub class.
 *
 * @author Hiroyuki Wada
 */
public class GitHubExt extends GitHub {

    GitHubExt(String apiUrl, HttpConnector connector, RateLimitHandler rateLimitHandler, AbuseLimitHandler abuseLimitHandler, GitHubRateLimitChecker rateLimitChecker, AuthorizationProvider authorizationProvider) throws IOException {
        super(apiUrl, connector, rateLimitHandler, abuseLimitHandler, rateLimitChecker, authorizationProvider);
    }

    public static GitHubExt build(GitHubBuilder build) throws IOException {
        RateLimitHandler rateLimitHandler = RateLimitHandler.WAIT;
        AbuseLimitHandler abuseLimitHandler = AbuseLimitHandler.WAIT;
        GitHubRateLimitChecker rateLimitChecker = new GitHubRateLimitChecker();

        GitHub gitHub = build.build();

        return new GitHubExt(gitHub.getApiUrl(), gitHub.getConnector(), rateLimitHandler, abuseLimitHandler, rateLimitChecker,
                build.authorizationProvider);
    }

    /**
     * Unofficial fetch user API.
     * https://stackoverflow.com/questions/11976393/get-github-username-by-id/30579888#30579888
     * https://github.community/t/official-support-for-looking-up-resources-by-id/119703
     *
     * @param id GitHub user's database ID
     * @return GitHub user object
     * @throws IOException the io exception
     */
    public GHUser getUser(long id) throws IOException {
        GHUser u = createRequest().withUrlPath("/user/" + String.valueOf(id)).fetch(GHUser.class);
        u.root = this;
        return u;
    }

    /**
     * Returns extension version of the GHOrganization.
     *
     * @param name GitHub organization name
     * @return GitHub organization object
     * @throws IOException the io exception
     */
    @Override
    public GHOrganizationExt getOrganization(String name) throws IOException {
        GHOrganizationExt o = createRequest().withUrlPath("/orgs/" + name).fetch(GHOrganizationExt.class).wrapUp(this);
        return o;
    }

    /**
     * Returns GHEnterprise..
     *
     * @param name GitHub enterprise slug
     * @return GitHub enterprise object
     */
    public GHEnterpriseExt getEnterprise(String name) {
        GHEnterpriseExt enterprise = new GHEnterpriseExt();
        enterprise.root = this;
        enterprise.login = name;
        return enterprise;
    }
}
