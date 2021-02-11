package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of a GraphQL edge.
 *
 * @param <T> the type parameter
 * @author Hiroyuki Wada
 */
public class GraphQLEdge<T> {

    @JsonProperty("cursor")
    public String cursor;

    @JsonProperty("node")
    public T node;
}
