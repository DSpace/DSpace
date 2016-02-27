/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.solr.client.solrj.util.ClientUtils;

import org.dspace.core.Context;
import org.dspace.xoai.data.DSpaceDatabaseItem;

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
    public boolean isShown(DSpaceDatabaseItem item)
    {
        if (item.getDatestamp().compareTo(_date) <= 0)
            return true;
        return false;
    }

    private String dateToString(Date date)
    {
        SimpleDateFormat formatDate = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'");
        return formatDate.format(date);
    }

    @Override
    public SolrFilterResult getQuery()
    {
        return new SolrFilterResult("item.lastmodified:[* TO "
                + ClientUtils.escapeQueryChars(this.dateToString(_date)) + "]");
    }

}
