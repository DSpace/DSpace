package org.dspace.search;

import java.util.List;

public class QueryResults
{
    private int hitCount;       // total hits returned by search engine
    private int start;          // offset of query 'page'
    private int pageSize;       // max number of hits returned

    private List hitHandles;    // handles of content (items, collections, communities)
    private List hitTypes;      // Integers from Constants defng types of corresponding handles 

    /** set total number of hits found by search engine, not number in hitHandles */
    public void setHitCount( int newCount ) { hitCount = newCount; }
    
    /** get total number of hits found by search engine, not just number of returned results */
    public int  getHitCount() { return hitCount; }
    
    /** set start of 'page' of results */
    public void setStart( int newStart ) { start = newStart; }

    /** get start of 'page' of results */
    public int getStart() { return start; }
    
    /** set length of 'page' of results */
    public void setPageSize( int newSize ) { pageSize = newSize; }

    /** get length of 'page' of results */
    public int getPageSize() { return pageSize; }

    /** set the List of handles corresponding to hits */
    public void setHitHandles( List myHits ) { hitHandles = myHits; }
    
    /** get the List of handles corresponding to hits */
    public List getHitHandles() { return hitHandles; }

    /** set the List of types corresponding to handles */
    public void setHitTypes( List newTypes ) { hitTypes = newTypes; }
    
    /** get the List of types corresponding to handles */
    public List getHitTypes() { return hitTypes; }
}
