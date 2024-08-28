package org.kohsuke.github;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Used for any SCIM resource that has pagination information.
 * <p>
 * This class is not thread-safe. Any one instance should only be called from a single thread.
 *
 * @param <T> the type parameter
 * @author Hiroyuki Wada
 */
public class SCIMPageIterator<T extends SCIMSearchResult> implements Iterator<T> {

    private final GitHubClient client;
    private final Class<T> type;

    private T next;

    private GitHubRequest nextRequest;

    private GitHubResponse<T> finalResponse = null;

    private SCIMPageIterator(GitHubClient client, Class<T> type, GitHubRequest request) {
        if (!"GET".equals(request.method())) {
            throw new IllegalStateException("Request method \"GET\" is required for page iterator.");
        }

        this.client = client;
        this.type = type;
        this.nextRequest = request;
    }

    static <T extends SCIMSearchResult> SCIMPageIterator<T> create(GitHubClient client, Class<T> type, GitHubRequest request, int pageSize, int pageOffset) {

        try {
            if (pageSize > 0) {
                GitHubRequest.Builder<?> builder = request.toBuilder().with("count", pageSize);
                if (pageOffset > 0) {
                    builder.with("startIndex", pageOffset);
                }
                request = builder.build();
            }

            return new SCIMPageIterator<>(client, type, request);
        } catch (MalformedURLException e) {
            throw new GHException("Unable to build GitHub SCIM API URL", e);
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
            nextRequest = findNextURL(nextResponse);
            if (nextRequest == null) {
                finalResponse = nextResponse;
            }
        } catch (IOException e) {
            // Iterators do not throw IOExceptions, so we wrap any IOException
            // in a runtime GHException to bubble out if needed.
            throw new GHException("Failed to retrieve " + url, e);
        }
    }

    private GitHubRequest findNextURL(GitHubResponse<T> nextResponse) throws MalformedURLException {
        T res = nextResponse.body();
        long endIndex = res.startIndex + res.itemsPerPage;
        if (endIndex > res.totalResults) {
            // No more pages
            return null;
        }

        // Build request for next page
        return nextResponse.request().toBuilder().set("startIndex", endIndex).build();
    }
}
