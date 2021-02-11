package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of a GraphQL pageInfo
 *
 * @param <T> the type parameter
 * @author Hiroyuki Wada
 */
public class GraphQLPageInfo<T> {
    @JsonProperty("endCursor")
    public String endCursor;

    @JsonProperty("hasNextPage")
    public boolean hasNextPage;

    @JsonProperty("hasPreviousPage")
    public boolean hasPreviousPage;

    @JsonProperty("startCursor")
    public String startCursor;
}
