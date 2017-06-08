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

    private T nextObject;
    private boolean retrievedObject = false;


    public HibernateScrollableResultsIterator(Query query) {
        this.scrollableResults = query.scroll(ScrollMode.FORWARD_ONLY);
    }

    @Override
    public boolean hasNext() {
        // if we have a next element that was not pulled, just simply return true.
      		if(!retrievedObject && nextObject != null){
      			return true;
      		}

      		if(scrollableResults.next()){
      			//we remember the element
                nextObject = (T)scrollableResults.get(0);
      			retrievedObject = false;
      			return true;
      		}else{
                scrollableResults.close();
            }
      		return false;
    }

    @Override
    public T next() {
        T toReturn = null;
      		//next variable could be null because the last element was sent or because we iterate on the next()
      		//instead of hasNext()
      		if(nextObject == null){
      			//if we can retrieve an element, do it
      			if(scrollableResults.next()){
      				toReturn = (T)scrollableResults.get(0);
      			}
      		}
      		else{
                //the element was retrieved by hasNext, return it
      			toReturn = nextObject;
                nextObject = null;
      		}

      		retrievedObject = true;
      		return toReturn;
    }

    @Override
    public void remove() {
        //Don't need this method, we are using hibernate scrolling with a forward only, so a remove has no effect.
    }
}