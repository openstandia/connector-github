package org.kohsuke.github;


import java.net.MalformedURLException;
import java.util.function.Function;

public abstract class GraphQLSearchBuilder<T, U extends GraphQLEdge, V extends GraphQLSearchVariables> extends GHQueryBuilder<T> {
    protected final V variables;

    /**
     * Data transfer object that receives the result of search.
     */
    private final Class<? extends GraphQLSearchResult<T>> receiverType;

    protected final GHOrganization organization;

    GraphQLSearchBuilder(GitHub root, GHOrganization org, Class<? extends GraphQLSearchResult<T>> receiverType) {
        super(root);
        this.organization = org;
        this.receiverType = receiverType;
        req.withUrlPath(getApiUrl());
        req.rateLimit(RateLimitTarget.GRAPHQL);
        req.method("POST");
        req.set("query", getQuery());
        this.variables = initSearchVariables();
    }

    /**
     * Performs the search.
     */
    public GraphQLPagedSearchIterable list() {
        try {
            return new GraphQLPagedSearchIterable(root, req.build(), receiverType, variables, getEdges(), getPageInfo());
        } catch (MalformedURLException e) {
            throw new GHException("", e);
        }
    }

    /**
     * Gets api url.
     *
     * @return the api url
     */
    protected String getApiUrl() {
        return "/graphql";
    }

    protected abstract String getQuery();

    protected abstract Function<GraphQLSearchResult<T>, GraphQLPageInfo> getPageInfo();

    protected abstract Function<GraphQLSearchResult<T>, U[]> getEdges();

    protected V initSearchVariables() {
        return (V) new GraphQLSearchVariables();
    }
}