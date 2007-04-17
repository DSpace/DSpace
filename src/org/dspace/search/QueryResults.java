/*
 * QueryResults.java
 *
 * $Id: QueryResults.java,v 1.5 2004/12/22 17:48:37 jimdowning Exp $
 *
 * Version: $Revision: 1.5 $
 *
 * Date: $Date: 2004/12/22 17:48:37 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.search;

import java.util.List;

/**
 * Contains the results of a query. Use access methods to examine and retrieve
 * the results.
 */
public class QueryResults
{
    private int hitCount; // total hits returned by search engine

    private int start; // offset of query 'page'

    private int pageSize; // max number of hits returned

    private List hitHandles; // handles of content (items, collections,
                             // communities)

    private List hitTypes; // Integers from Constants defng types of
                           // corresponding handles

    private String errorMsg; //error string, if there is one

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
    public void setHitHandles(List myHits)
    {
        hitHandles = myHits;
    }

    /** get the List of handles corresponding to hits */
    public List getHitHandles()
    {
        return hitHandles;
    }

    /** set the List of types corresponding to handles */
    public void setHitTypes(List newTypes)
    {
        hitTypes = newTypes;
    }

    /** get the List of types corresponding to handles */
    public List getHitTypes()
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