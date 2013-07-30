/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import java.util.Date;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.core.Context;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.util.DateUtils;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DateUntilFilter extends DSpaceFilter
{
    private Date _date;

    public DateUntilFilter(Date date)
    {
        _date = date;
    }

    @Override
    public DatabaseFilterResult getWhere(Context context)
    {
        return new DatabaseFilterResult("i.last_modified <= ?",
                new java.sql.Date(_date.getTime()));
    }

    @Override
    public boolean isShown(DSpaceItem item)
    {
        if (item.getDatestamp().compareTo(_date) <= 0)
            return true;
        return false;
    }

    @Override
    public SolrFilterResult getQuery()
    {
        return new SolrFilterResult("item.lastmodified:[* TO "
                + ClientUtils.escapeQueryChars(DateUtils.formatToSolr(_date, false)) + "]");
    }

}
