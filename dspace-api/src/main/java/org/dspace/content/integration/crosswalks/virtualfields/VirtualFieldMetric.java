/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.metrics.CrisItemMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link VirtualField} that returns metric count by metric type configured.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class VirtualFieldMetric implements VirtualField {

    private final static Logger LOGGER = LoggerFactory.getLogger(VirtualFieldMetric.class);

    @Autowired
    private CrisItemMetricsService crisItemMetricsService;

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {
        String[] virtualFieldName = fieldName.split("\\.", 3);

        if (virtualFieldName.length != 3) {
            LOGGER.warn("Invalid metric virtual field: " + fieldName);
            return new String[] {};
        }

        List<CrisMetrics> metrics = crisItemMetricsService.getMetrics(context, item.getID());
        for (CrisMetrics metric : metrics) {
            if (StringUtils.equals(virtualFieldName[2], metric.getMetricType())) {
                return new String[] { metric.getMetricCount().toString() };
            }
        }
        return new String[] {};
    }

}