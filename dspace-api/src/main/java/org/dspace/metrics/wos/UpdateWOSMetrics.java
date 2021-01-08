/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.wos;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.metrics.scopus.CrisMetricDTO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateWOSMetrics extends AbstractUpdateWOSMetrics {

    private static final Logger log = LogManager.getLogger(UpdateWOSMetrics.class);

    public static final String WOS_METRIC_TYPE = "wosCitation";

    @Autowired
    private WOSProvider wosProvider;

    @Override
    public List<String> getFilters() {
        return Arrays.asList("relationship.type:Publication", "dc.identifier.doi:*");
    }

    @Override
    public boolean updateMetric(Context context, Item item, String param) {
        CrisMetricDTO metricDTO = new CrisMetricDTO();
        String doi = itemService.getMetadataFirstValue(item, "dc", "identifier", "doi", Item.ANY);
        if (StringUtils.isNotBlank(doi)) {
            metricDTO = wosProvider.getWOSObject(doi);
        }
        return updateWosMetric(context, item, metricDTO);
    }

}