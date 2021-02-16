package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of a SCIM search
 *
 * @param <T> the type parameter
 * @author Hiroyuki Wada
 */
public class SCIMSearchResult<T> {
    @JsonProperty("totalResults")
    public int totalResults;
    @JsonProperty("itemsPerPage")
    public int itemsPerPage;
    @JsonProperty("startIndex")
    public int startIndex;
    @JsonProperty("Resources")
    public T[] Resources;
}
