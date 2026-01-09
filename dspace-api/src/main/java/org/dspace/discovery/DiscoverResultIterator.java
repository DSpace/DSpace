/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

/**
 * Implementation of {@link Iterator} to iterate over the discover search result.
 *
 * @param <T>  the type of the indexed object
 * @param <PK> the type of the id of the indexed object
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DiscoverResultIterator<T extends ReloadableEntity, PK extends Serializable> implements Iterator<T> {

    private final SearchService searchService;

    private final Context context;

    private final IndexableObject<?, ?> scopeObject;

    private final DiscoverQuery discoverQuery;
    private final boolean uncacheEntitites;
    private final int maxResults;
    private int iteratorCounter;
    private DiscoverResult currentDiscoverResult;
    private Iterator<IndexableObject> currentSlotIterator;

    /**
     * Constructor for global search without scope and unlimited results.
     */
    public DiscoverResultIterator(Context context, DiscoverQuery discoverQuery) {
        this(context, null, discoverQuery, true, -1);
    }

    /**
     * Constructor with optional uncacheEntities flag.
     */
    public DiscoverResultIterator(Context context, DiscoverQuery discoverQuery, boolean uncacheEntities) {
        this(context, null, discoverQuery, uncacheEntities, -1);
    }

    /**
     * Constructor for scoped search without a limit on the number of results.
     */
    public DiscoverResultIterator(Context context, IndexableObject<?, ?> scopeObject, DiscoverQuery discoverQuery) {
        this(context, scopeObject, discoverQuery, true, -1);
    }

    /**
     * Full constructor with all options.
     */
    public DiscoverResultIterator(Context context, IndexableObject<?, ?> scopeObject, DiscoverQuery discoverQuery,
                                  boolean uncacheEntities, int maxResults) {

        this.context = context;
        this.scopeObject = scopeObject;
        this.discoverQuery = discoverQuery;
        this.iteratorCounter = discoverQuery.getStart();
        this.searchService = SearchUtils.getSearchService();
        this.uncacheEntitites = uncacheEntities;
        this.maxResults = maxResults;

        updateCurrentSlotIterator();
    }

    /**
     * Checks if there are more elements to iterate over.
     * If the maximum number of results has been reached, it returns false.
     * Otherwise, it checks the current slot iterator or fetches the next batch of results.
     *
     * @return true if there are more elements, false otherwise
     */
    @Override
    public boolean hasNext() {
        if (maxResults > 0 && iteratorCounter >= maxResults) {
            return false;
        }
        if (currentSlotIterator.hasNext()) {
            return true;
        }

        this.discoverQuery.setStart(iteratorCounter);

        if (uncacheEntitites) {
            uncacheEntitites();
        }

        updateCurrentSlotIterator();

        return currentSlotIterator.hasNext();
    }

    /**
     * Returns the next element in the iteration.
     */
    @Override
    public T next() {
        return (T) getNextIndexableObject().getIndexedObject();
    }

    /**
     * Returns the total number of search results.
     * <p>
     * This method calculates the total number of search results based on the current search query.
     * If the slot iterator has not been initialized, it updates the iterator.
     * </p>
     *
     * @return the total number of search results as a {@code long}
     */
    public long getTotalSearchResults() {

        if (currentSlotIterator == null) {
            updateCurrentSlotIterator();
        }

        return this.currentDiscoverResult.getTotalSearchResults();
    }

    /**
     * Retrieves the next indexable object.
     * Throws NoSuchElementException if there are no more elements.
     */
    protected IndexableObject getNextIndexableObject() {

        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        iteratorCounter++;
        return currentSlotIterator.next();
    }

    private void uncacheEntitites() {
        List<IndexableObject> indexableObjects = currentDiscoverResult.getIndexableObjects();
        for (IndexableObject indexableObj : indexableObjects) {
            try {
                context.uncacheEntity(indexableObj.getIndexedObject());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void updateCurrentSlotIterator() {
        this.currentDiscoverResult = search();
        this.currentSlotIterator = currentDiscoverResult.getIndexableObjects().iterator();
    }

    private DiscoverResult search() {
        try {

            if (scopeObject == null) {
                return searchService.search(context, discoverQuery);
            } else {
                return searchService.search(context, scopeObject, discoverQuery);
            }

        } catch (SearchServiceException e) {
            throw new RuntimeException(e);
        }
    }


}
