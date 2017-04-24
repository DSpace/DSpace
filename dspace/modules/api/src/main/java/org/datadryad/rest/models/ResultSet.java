package org.datadryad.rest.models;

import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by daisie on 4/21/17.
 */
public class ResultSet {
    public int previousCursor = 0;
    public int currentCursor = 0;
    public int nextCursor = 0;
    public int firstCursor = 0;
    public int lastCursor = 0;
    public int pageSize = 20;
    public ArrayList<Integer> itemList;
    private static final Logger log = Logger.getLogger(ResultSet.class);

    public ResultSet(Collection<Integer> items, int pgsize) {
        pageSize = pgsize;
        itemList = new ArrayList<Integer>(items);
        Collections.sort(itemList);
        log.debug("items in result set: " + itemList.toString());
        adjustCursors(0);
    }

    public void adjustCursors(int currCursor) {
        if (currCursor <= 0) {
            currentCursor = itemList.get(0);
        } else {
            currentCursor = currCursor;
        }
        int currentIndex = itemList.indexOf(currentCursor);
        if (currentIndex + pageSize < itemList.size()) {
            // we have a valid next page
            nextCursor = itemList.get(currentIndex + pageSize);
        } else {
            nextCursor = itemList.get(itemList.size()-1);
        }
        if (currentIndex - pageSize > 0) {
            // we have a valid previous page
            previousCursor = itemList.get(currentIndex - pageSize);
        } else {
            previousCursor = itemList.get(0);
        }
        firstCursor = itemList.get(0);
        lastCursor = firstCursor;
        if (itemList.size() > pageSize) {
            lastCursor = itemList.get(itemList.size() - pageSize - 1);
        }

        log.debug("cursors are " + previousCursor + ", " + currentCursor + ", " + nextCursor);
    }

    public List<Integer> getCurrentSet(int currentCursor) {
        adjustCursors(currentCursor);
        int indexFrom = itemList.indexOf(currentCursor);
        int indexTo = itemList.indexOf(nextCursor);
        if (indexFrom <= 0) {
            indexFrom = 0;
        }
        if (indexTo >= itemList.size()) {
            indexTo = itemList.size();
        }
        log.debug("list from " + indexFrom + " to " + indexTo);
        return itemList.subList(indexFrom, indexTo);
    }
}
