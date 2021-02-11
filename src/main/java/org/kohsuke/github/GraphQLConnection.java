package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of a GraphQL connection.
 *
 * @param <T> the type parameter
 * @author Hiroyuki Wada
 */
public class GraphQLConnection<T extends GraphQLEdge> {
    @JsonProperty("edges")
    public T[] edges;

    @JsonProperty("pageInfo")
    public GraphQLPageInfo pageInfo;

    @JsonProperty("totalCount")
    public int totalCount;
}
