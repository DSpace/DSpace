/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import java.time.Instant;
import java.time.temporal.ChronoField;

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
    private final Instant date;

    public DateFromFilter(Instant date) {
        // As this is a 'from' filter, ensure milliseconds are set to zero (minimum value)
        this.date = date.with(ChronoField.MILLI_OF_SECOND, 0);
    }

    @Override
    public boolean isShown(DSpaceItem item) {
        if (!item.getDatestamp().toInstant().isBefore(date)) {
            return true;
        }
        return false;
    }

    @Override
    public SolrFilterResult buildSolrQuery() {
        // Tweak to set the milliseconds
        String format = dateProvider.format(java.util.Date.from(date)).replace("Z", ".000Z");
        return new SolrFilterResult("item.lastmodified:["
                                        + ClientUtils.escapeQueryChars(format)
                                        + " TO *]");
    }

}
