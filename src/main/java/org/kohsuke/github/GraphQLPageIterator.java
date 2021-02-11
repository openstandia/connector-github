package org.kohsuke.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Used for any GraphQL resource that has pagination information.
 * <p>
 * This class is not thread-safe. Any one instance should only be called from a single thread.
 *
 * @param <T> the type parameter
 * @author Hiroyuki Wada
 */
public class GraphQLPageIterator<T extends GraphQLSearchResult<U>, U> implements Iterator<T> {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final GitHubClient client;
    private final Class<T> type;
    private final GraphQLSearchVariables variables;
    private Function<GraphQLSearchResult<U>, GraphQLPageInfo> findNext;

    private T next;

    private GitHubRequest nextRequest;

    private GitHubResponse<T> finalResponse = null;

    private GraphQLPageIterator(GitHubClient client, Class<T> type, GitHubRequest request, GraphQLSearchVariables variables,
                                Function<GraphQLSearchResult<U>, GraphQLPageInfo> nextFinder) {
        if (!"POST".equals(request.method())) {
            throw new IllegalStateException("Request method \"POST\" is required for GraphQL page iterator.");
        }

        this.client = client;
        this.type = type;
        this.nextRequest = request;
        this.variables = variables;
        this.findNext = nextFinder;
    }

    static <T extends GraphQLSearchResult<U>, U> GraphQLPageIterator create(GitHubClient client, Class<T> type,
                                                                            GitHubRequest request, GraphQLSearchVariables variables,
                                                                            Function<GraphQLSearchResult<U>, GraphQLPageInfo> nextFinder) {

        try {
            GitHubRequest.Builder<?> builder = request.toBuilder().set("variables", mapper.writeValueAsString(variables));
            request = builder.build();

            return new GraphQLPageIterator<>(client, type, request, variables, nextFinder);
        } catch (MalformedURLException | JsonProcessingException e) {
            throw new GHException("Unable to build GitHub GraphQL API URL", e);
        }
    }

    public boolean hasNext() {
        fetch();
        return next != null;
    }

    public T next() {
        fetch();
        T result = next;
        if (result == null)
            throw new NoSuchElementException();
        // If this is the last page, keep the response
        next = null;
        return result;
    }

    public GitHubResponse<T> finalResponse() {
        if (hasNext()) {
            throw new GHException("Final response is not available until after iterator is done.");
        }
        return finalResponse;
    }

    private void fetch() {
        if (next != null)
            return; // already fetched
        if (nextRequest == null)
            return; // no more data to fetch

        URL url = nextRequest.url();
        try {
            GitHubResponse<T> nextResponse = client.sendRequest(nextRequest,
                    (responseInfo) -> GitHubResponse.parseBody(responseInfo, type));
            assert nextResponse.body() != null;
            next = nextResponse.body();

            if (next == null) {
                throw new GHException("GraphQL API returns error");
            }

            GraphQLPageInfo pageInfo = findNext.apply(nextResponse.body());
            if (pageInfo == null || !pageInfo.hasNextPage) {
                finalResponse = nextResponse;
                nextRequest = null;
                return;
            }

            GraphQLSearchVariables nextVariables = variables.next(pageInfo);

            nextRequest = nextResponse.request().toBuilder()
                    .set("variables", mapper.writeValueAsString(nextVariables))
                    .build();

        } catch (IOException e) {
            // Iterators do not throw IOExceptions, so we wrap any IOException
            // in a runtime GHException to bubble out if needed.
            throw new GHException("Failed to retrieve " + url, e);
        }
    }
}
