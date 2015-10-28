/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

import java.util.Iterator;

/**
 * Iterator implementation that allows for forward scrolling through a query. An iterator is still returned to the UI,
 * while in the backend the hibernate scrolling can be used without tying the DSpace to the hibernate implementation.
 *
 * @author kevinvandevelde at atmire.com
 */
public class HibernateScrollableResultsIterator<T> implements Iterator<T> {

    private ScrollableResults scrollableResults;

    public HibernateScrollableResultsIterator(Query query) {
        this.scrollableResults = query.scroll(ScrollMode.FORWARD_ONLY);
    }

    @Override
    public boolean hasNext() {
        return !scrollableResults.isLast();
    }

    @Override
    public T next() {
        scrollableResults.next();
        @SuppressWarnings("unchecked")
        T nextObject = (T) scrollableResults.get(0);
        return nextObject;
    }

    @Override
    public void remove() {
        //Don't need this method, we are using hibernate scrolling with a forward only, so a remove has no effect.
    }
}