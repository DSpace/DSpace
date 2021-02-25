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
public class DateUntilFilter extends DSpaceFilter {
    private static final DateProvider dateProvider = new BaseDateProvider();
    private final Date date;

    public DateUntilFilter(Date date) {
        Calendar calendar =  Calendar.getInstance();
        calendar.setTime(date);
        // As this is an 'until' filter, ensure milliseconds are set to 999 (maximum value)
        calendar.set(Calendar.MILLISECOND, 999);
        this.date = calendar.getTime();
    }

    @Override
    public boolean isShown(DSpaceItem item) {
        if (item.getDatestamp().compareTo(date) <= 0) {
            return true;
        }
        return false;
    }

    @Override
    public SolrFilterResult buildSolrQuery() {
        String format = dateProvider.format(date).replace("Z", ".999Z"); // Tweak to set the milliseconds
        // if date has timestamp of 00:00:00, switch it to refer to end of day
        if (format.substring(11, 19).equals("00:00:00")) {
            format = format.substring(0, 11) + "23:59:59" + format.substring(19);
        }
        return new SolrFilterResult("item.lastmodified:[* TO "
                                        + ClientUtils.escapeQueryChars(format) + "]");
    }

}
