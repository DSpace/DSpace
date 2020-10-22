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

import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;

/**
 * Implementation of {@link Iterator} to iterate over the discover search result.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 * @param <T> the type of the indexed object
 * @param <PK> the type of the id of the indexed object
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class DiscoverResultIterator<T extends ReloadableEntity, PK extends Serializable> implements Iterator<T> {

    private SearchService searchService;

    private Context context;

    private IndexableObject<?, ?> scopeObject;

    private DiscoverQuery discoverQuery;

    private int iteratorCounter;

    private DiscoverResult currentDiscoverResult;

    private Iterator<IndexableObject> currentSlotIterator;

    public DiscoverResultIterator(Context context, DiscoverQuery discoverQuery) {
        this(context, null, discoverQuery);
    }

    public DiscoverResultIterator(Context context, IndexableObject<?, ?> scopeObject, DiscoverQuery discoverQuery) {

        this.context = context;
        this.scopeObject = scopeObject;
        this.discoverQuery = discoverQuery;
        this.iteratorCounter = discoverQuery.getStart();
        this.searchService = SearchUtils.getSearchService();

        updateCurrentSlotIterator();
    }

    @Override
    public boolean hasNext() {
        if (currentSlotIterator.hasNext()) {
            return true;
        }

        this.discoverQuery.setStart(iteratorCounter);

        uncacheEntitites();
        updateCurrentSlotIterator();

        return currentSlotIterator.hasNext();
    }
    @Override
    public T next() {
        T nextElement = (T) currentSlotIterator.next().getIndexedObject();
        iteratorCounter++;
        return nextElement;
    }

    public long getTotalSearchResults() {
        return this.currentDiscoverResult.getTotalSearchResults();
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
