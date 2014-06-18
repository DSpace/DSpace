/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.search;

import java.util.ArrayList;
import java.util.List;

import org.dspace.core.ConfigurationManager;

/**
 * Contains the results of a query. Use access methods to examine and retrieve
 * the results.
 * 
 * @deprecated Since DSpace 4 the system use an abstraction layer named
 *             Discovery to provide access to different search provider. The
 *             legacy system build upon Apache Lucene is likely to be removed in
 *             a future version. If you are interested in use Lucene as backend
 *             for the DSpace search system please consider to build a Lucene
 *             implementation of the Discovery interfaces
 */
@Deprecated
public class QueryResults
{
    private long queryTime; // time to search (ms)
    
    private int hitCount; // total hits returned by search engine

    private int start; // offset of query 'page'

    private int pageSize; // max number of hits returned

    private List<String> hitHandles = new ArrayList<String>(); // handles of content (items, collections, communities)

    private List<Integer> hitTypes = new ArrayList<Integer>(); // Resource type - from Constants
    private List<Integer> hitIds   = new ArrayList<Integer>(); // Resource ids

    private String errorMsg; //error string, if there is one

    /** number of metadata elements to display before truncating using "et al" */
    private int etAl = ConfigurationManager.getIntProperty("webui.itemlist.author-limit");

    public long getQueryTime()
    {
        return queryTime;
    }
    
    public void setQueryTime(long queryTime)
    {
        this.queryTime = queryTime;
    }
    
    /**
     * @return  the number of metadata fields at which to truncate with "et al"
     */
    public int getEtAl()
    {
        return etAl;
    }

    /**
     * set the number of metadata fields at which to truncate with "et al"
     *
     * @param etAl
     */
    public void setEtAl(int etAl)
    {
        this.etAl = etAl;
    }

    /** set total number of hits found by search engine, not number in hitHandles */
    public void setHitCount(int newCount)
    {
        hitCount = newCount;
    }

    /**
     * get total number of hits found by search engine, not just number of
     * returned results
     */
    public int getHitCount()
    {
        return hitCount;
    }

    /** set start of 'page' of results */
    public void setStart(int newStart)
    {
        start = newStart;
    }

    /** get start of 'page' of results */
    public int getStart()
    {
        return start;
    }

    /** set length of 'page' of results */
    public void setPageSize(int newSize)
    {
        pageSize = newSize;
    }

    /** get length of 'page' of results */
    public int getPageSize()
    {
        return pageSize;
    }

    /** set the List of handles corresponding to hits */
    public void setHitHandles(List<String> myHits)
    {
        hitHandles = myHits != null ? myHits : new ArrayList<String>();
    }

    /** get the List of handles corresponding to hits */
    public List<String> getHitHandles()
    {
        return hitHandles;
    }

    /** set the List of ids corresponding to hits */
    public void setHitIds(List<Integer> myHits)
    {
        hitIds = myHits != null ? myHits : new ArrayList<Integer>();
    }

    /** get the List of handles corresponding to hits */
    public List<Integer> getHitIds()
    {
        return hitIds;
    }

    /** set the List of types corresponding to handles */
    public void setHitTypes(List<Integer> newTypes)
    {
        hitTypes = newTypes != null ? newTypes : new ArrayList<Integer>();
    }

    /** get the List of types corresponding to handles */
    public List<Integer> getHitTypes()
    {
        return hitTypes;
    }

    /** set error message */
    public void setErrorMsg(String msg)
    {
        errorMsg = msg;
    }

    /** get error message */
    public String getErrorMsg()
    {
        return errorMsg;
    }
}
