/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DatabaseFilterResult
{
    private String _where;

    private List<Object> _params;

    private boolean _nothing;

    public DatabaseFilterResult()
    {
        _nothing = true;
    }

    public DatabaseFilterResult(String where, Object... params)
    {
        _nothing = false;
        _where = where;
        _params = new ArrayList<Object>();
        for (Object obj : params)
            _params.add(obj);
    }

    public DatabaseFilterResult(String where, List<Object> params)
    {
        _nothing = false;
        _where = where;
        _params = params;
    }

    public boolean hasResult()
    {
        return !_nothing;
    }

    public String getWhere()
    {
        return _where;
    }

    public List<Object> getParameters()
    {
        return _params;
    }

}
