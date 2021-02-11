package org.kohsuke.github;

import java.util.Iterator;

/**
 * {@link PagedIterable} enhanced to report search result specific information.
 *
 * @param <T> the type parameter
 * @author Hiroyuki Wada
 */
public class SCIMPagedSearchIterable<T> extends PagedIterable<T> {
    private final transient GitHub root;

    private final GitHubRequest request;

    private final Class<? extends SCIMSearchResult<T>> receiverType;

    /**
     * As soon as we have any result fetched, it's set here so that we can report the total count.
     */
    private SCIMSearchResult<T> result;

    public SCIMPagedSearchIterable(GitHub root, GitHubRequest request, Class<? extends SCIMSearchResult<T>> receiverType) {
        this.root = root;
        this.request = request;
        this.receiverType = receiverType;
    }

    @Override
    public SCIMPagedSearchIterable<T> withPageSize(int size) {
        return (SCIMPagedSearchIterable<T>) super.withPageSize(size);
    }

    /**
     * Returns the total number of hit, including the results that's not yet fetched.
     *
     * @return the total count
     */
    public int getTotalCount() {
        populate();
        return result.totalResults;
    }

    /**
     * Is incomplete boolean.
     *
     * @return the boolean
     */
    public boolean isIncomplete() {
        populate();
        return result.totalResults <= result.startIndex + result.itemsPerPage;
    }

    private void populate() {
        if (result == null)
            iterator().hasNext();
    }

    @Override
    public PagedIterator<T> _iterator(int pageSize) {
        final Iterator<T[]> adapter = adapt(
                SCIMPageIterator.create(root.getClient(), receiverType, request, pageSize));
        return new PagedIterator<T>(adapter, null);
    }

    /**
     * Adapts {@link Iterator}.
     *
     * @param base the base
     * @return the iterator
     */
    protected Iterator<T[]> adapt(final Iterator<? extends SCIMSearchResult<T>> base) {
        return new Iterator<T[]>() {
            public boolean hasNext() {
                return base.hasNext();
            }

            public T[] next() {
                SCIMSearchResult<T> v = base.next();
                if (result == null)
                    result = v;
                return v.Resources;
            }
        };
    }
}
