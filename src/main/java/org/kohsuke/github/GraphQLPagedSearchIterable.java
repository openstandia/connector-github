package org.kohsuke.github;

import java.util.Iterator;
import java.util.function.Function;

/**
 * {@link PagedIterable} enhanced to report search result specific information.
 *
 * @param <T> the type parameter
 * @author Hiroyuki Wada
 */
public class GraphQLPagedSearchIterable<T, U extends GraphQLEdge> extends PagedIterable<U> {
    private final transient GitHub root;

    private final GitHubRequest request;

    private final Class<? extends GraphQLSearchResult<T>> receiverType;

    /**
     * As soon as we have any result fetched, it's set here so that we can report the total count.
     */
    private GraphQLSearchResult<T> result;

    private final GraphQLSearchVariables variables;
    private final Function<GraphQLSearchResult<T>, U[]> adaptor;
    private final Function<GraphQLSearchResult<T>, GraphQLPageInfo> nextFinder;

    public GraphQLPagedSearchIterable(GitHub root, GitHubRequest request, Class<? extends GraphQLSearchResult<T>> receiverType,
                                      GraphQLSearchVariables variables,
                                      Function<GraphQLSearchResult<T>, U[]> adaptor,
                                      Function<GraphQLSearchResult<T>, GraphQLPageInfo> nextFinder) {
        this.root = root;
        this.request = request;
        this.receiverType = receiverType;
        this.variables = variables;
        this.adaptor = adaptor;
        this.nextFinder = nextFinder;
    }

    @Override
    public GraphQLPagedSearchIterable<T, U> withPageSize(int size) {
        return (GraphQLPagedSearchIterable<T, U>) super.withPageSize(size);
    }

    @Override
    public PagedIterator<U> _iterator(int pageSize) {
        variables.first = pageSize;
        final Iterator<U[]> adapter = adapt(
                GraphQLPageIterator.create(root.getClient(), receiverType, request, variables, nextFinder));
        return new PagedIterator<U>(adapter, null);
    }

    /**
     * Adapts {@link Iterator}.
     *
     * @param base the base
     * @return the iterator
     */
    protected Iterator<GraphQLEdge<U>[]> adapt(final Iterator<? extends GraphQLSearchResult<T>> base) {
        return new Iterator<GraphQLEdge<U>[]>() {
            public boolean hasNext() {
                return base.hasNext();
            }

            public GraphQLEdge<U>[] next() {
                GraphQLSearchResult<T> v = base.next();
                if (result == null)
                    result = v;
                return adaptor.apply(v);
            }
        };
    }
}
