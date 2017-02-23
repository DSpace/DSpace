/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * 
 * @author Lyncode Development Team (dspace at lyncode dot com)
 */
public class SolrFilterResult
{
	private static Logger log = LogManager.getLogger(SolrFilterResult.class);
    private String _where;

    private boolean _nothing;

    public SolrFilterResult()
    {
        _nothing = true;
    }

    public SolrFilterResult(String query)
    {
    	log.debug("XOAI SolrQuery: "+ query);
        _nothing = false;
        _where = query;
    }

    public boolean hasResult()
    {
        return !_nothing;
    }

    public String getQuery()
    {
        return _where;
    }
}
