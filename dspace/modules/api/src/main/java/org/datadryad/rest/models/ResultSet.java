package org.datadryad.rest.models;

import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by daisie on 4/21/17.
 *
 * ResultSets enable pagination by providing a mechanism to only manipulate the list of unique IDs for large datasets.
 *
 * The pagination is cursor-based, meaning that a pabe is based on the unique ID of the first item in the page,
 * not on a relative page number or place in the list.
 *
 * To generate a particular page of results, use getCurrentSet on a particular cursor to get that page's unique IDs,
 * then do whatever db queries or object accessing to generate just the objects needed for that current page.
 *
 * ResultSets are paged in ascending order by default. To make a descending-order ResultSet, setDescending().
 * The internal list is still in the order originally given to the ResultSet, but the pages move in the opposite
 * direction and each call to getCurrentSet returns that page's list in opposite order.
 */
public class ResultSet {
    private int previousCursor = 0;
    private int currentCursor = 0;
    private int nextCursor = 0;
    private int firstCursor = 0;
    private int lastCursor = 0;
    private int pageSize = 20;
    public ArrayList<Integer> itemList;
    private static final Logger log = Logger.getLogger(ResultSet.class);

    public ResultSet(Collection<Integer> items, Integer pgsize, Integer cursor) {
        if (pgsize != null && pgsize > 0) {
            pageSize = pgsize;
        }
        itemList = new ArrayList<Integer>(items);
        adjustCursors(cursor);
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getNextCursor() {
        return nextCursor;
    }

    public int getPreviousCursor() {
        return previousCursor;
    }

    public int getFirstCursor() {
        return firstCursor;
    }

    public int getLastCursor() {
        return lastCursor;
    }

    public int getCurrentCursor() {
        return currentCursor;
    }

    // the following methods should be used only for information, not as references. Use the cursor methods for pagination references.
    public int getCurrentIndex() {
        return itemList.indexOf(currentCursor);
    }

    public int getPreviousIndex() {
        return itemList.indexOf(previousCursor);
    }

    public int getNextIndex() {
        if (nextCursor == -1) {
            return itemList.size();
        }
        return itemList.indexOf(nextCursor);
    }

    public Boolean hasNextPage() {
        Boolean result = (getNextIndex() != -1) && (getNextIndex() < itemList.size() - 1);
        log.debug("nextCursor is " + nextCursor + ", nextIndex is " + getNextIndex() + ", hasNextPage = " + result);
        return result;
    }

    public Boolean hasPreviousPage() {
        Boolean result = getPreviousIndex() >= 0;
        log.debug("previousCursor is " + previousCursor + ", previousIndex is " + getPreviousIndex() + ", hasPreviousPage = " + result);
        return result;
    }


    public void adjustCursors(int currCursor) {
        if (itemList.size() == 0) {
            return;
        }
        if (currCursor <= 0) {
            currentCursor = itemList.get(0);
        } else {
            currentCursor = currCursor;
        }
        int currentIndex = itemList.indexOf(currentCursor);
        nextCursor = -1;
        if (currentIndex < itemList.size()) {
            // we have a valid next page
            if (currentIndex + pageSize <= itemList.size()) {
                 nextCursor = itemList.get(currentIndex + pageSize);
            } else {
                nextCursor = itemList.get(itemList.size() - 1);
            }
        }
        previousCursor = -1;
        if (currentIndex > 0) {
            // we have a valid previous page
            if (currentIndex - pageSize >= 0) {
                previousCursor = itemList.get(currentIndex - pageSize);
            } else {
                previousCursor = itemList.get(0);
            }
        }
        firstCursor = itemList.get(0);
        lastCursor = firstCursor;
        if (itemList.size() > pageSize) {
            lastCursor = itemList.get(itemList.size() - pageSize - 1);
        }

        log.debug("indices are " + getPreviousIndex() + ", " + getCurrentIndex() + ", " + getNextIndex());
        log.debug("hasPrevPage = " + hasPreviousPage() + ", hasNextPage = " + hasNextPage());
        log.debug("cursors are " + previousCursor + ", " + currentCursor + ", " + nextCursor);
    }

    public List<Integer> getCurrentSet(int currCursor) {
        List<Integer> resultList = new ArrayList<Integer>();
        if (itemList.size() <= pageSize) {
            resultList.addAll(itemList);
        } else {
            adjustCursors(currCursor);
            int indexA = getCurrentIndex();
            int indexB = getNextIndex();

            int indexFrom = (indexA <= indexB) ? indexA : indexB;
            int indexTo = (indexA > indexB) ? indexA : indexB;
            if (indexFrom <= 0) {
                indexFrom = 0;
            }
            if (indexTo >= itemList.size()) {
                indexTo = itemList.size();
            }
            log.debug("list from " + indexFrom + " to " + indexTo);
            for (int i = indexFrom+1; i <= indexTo; i++) {
                resultList.add(itemList.get(i));
            }
        }
        return resultList;
    }
}
