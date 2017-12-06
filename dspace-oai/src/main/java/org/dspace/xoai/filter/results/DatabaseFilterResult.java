/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter.results;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DatabaseFilterResult
{
    private String where;
    private List<Object> parameters;
    private boolean nothing;

    public DatabaseFilterResult()
    {
        nothing = true;
    }

    public DatabaseFilterResult(String where, Object... params)
    {
        nothing = false;
        this.where = where;
        parameters = new ArrayList<Object>();
        for (Object obj : params)
            parameters.add(obj);
    }

    public DatabaseFilterResult(String where, List<Object> params)
    {
        nothing = false;
        this.where = where;
        parameters = params;
    }

    public boolean hasResult()
    {
        return !nothing;
    }

    public String getQuery()
    {
        return where;
    }

    public List<Object> getParameters()
    {
        return parameters;
    }

}
