package org.kohsuke.github;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class SCIMSearchBuilder<T> extends GHQueryBuilder<T> {
    protected final Map<String, String> filter = new HashMap<>();

    /**
     * Data transfer object that receives the result of search.
     */
    private final Class<? extends SCIMSearchResult<T>> receiverType;

    protected final GHOrganization organization;

    SCIMSearchBuilder(GitHub root, GHOrganization org, Class<? extends SCIMSearchResult<T>> receiverType) {
        super(root);
        this.organization = org;
        this.receiverType = receiverType;
        req.withUrlPath(getApiUrl());
        req.rateLimit(RateLimitTarget.SEARCH);
    }

    /**
     * Search filter.
     *
     * @param key   the filter key
     * @param value the filter value
     * @return the gh query builder
     */
    public GHQueryBuilder<T> eq(String key, String value) {
        filter.put(key, value);
        return this;
    }

    /**
     * Performs the search.
     */
    @Override
    public SCIMPagedSearchIterable<T> list() {
        List<String> f = filter.entrySet().stream()
                .map(entry -> entry.getKey() + " eq \"" + escape(entry.getValue()) + "\"")
                .collect(Collectors.toList());

        if (!f.isEmpty()) {
            String filterStr = String.join(" and ", f);
            req.set("filter", filterStr);
        }

        try {
            return new SCIMPagedSearchIterable<>(root, req.build(), receiverType);
        } catch (MalformedURLException e) {
            throw new GHException("", e);
        }
    }

    private String escape(String value) {
        return value.replace("\"", "\\\"");
    }

    /**
     * Gets api url.
     *
     * @return the api url
     */
    protected abstract String getApiUrl();
}