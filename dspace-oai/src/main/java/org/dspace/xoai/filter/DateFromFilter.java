/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import java.util.Calendar;
import java.util.Date;

import com.lyncode.xoai.dataprovider.services.api.DateProvider;
import com.lyncode.xoai.dataprovider.services.impl.BaseDateProvider;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.filter.results.SolrFilterResult;

/**
 * @author Lyncode Development Team (dspace at lyncode dot com)
 */
public class DateFromFilter extends DSpaceFilter {
    private static final DateProvider dateProvider = new BaseDateProvider();
    private final Date date;

    public DateFromFilter(Date date) {
        Calendar calendar =  Calendar.getInstance();
        calendar.setTime(date);
        // As this is a 'from' filter, ensure milliseconds are set to zero (minimum value)
        calendar.set(Calendar.MILLISECOND, 0);
        this.date = calendar.getTime();
    }

    @Override
    public boolean isShown(DSpaceItem item) {
        if (item.getDatestamp().compareTo(date) >= 0) {
            return true;
        }
        return false;
    }

    @Override
    public SolrFilterResult buildSolrQuery() {
        String format = dateProvider.format(date).replace("Z", ".000Z"); // Tweak to set the milliseconds
        return new SolrFilterResult("item.lastmodified:["
                                        + ClientUtils.escapeQueryChars(format)
                                        + " TO *]");
    }

}
