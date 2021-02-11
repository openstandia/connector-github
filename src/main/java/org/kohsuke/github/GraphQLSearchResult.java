package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Represents the result of a GraphQL search.
 *
 * @param <T> the type parameter
 * @author Hiroyuki Wada
 */
public abstract class GraphQLSearchResult<T> {
    @JsonProperty("data")
    public abstract void setData(Map<String, T> data);

    public abstract T getData();
}
