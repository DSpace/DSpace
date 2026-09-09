/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.DynamicLayoutBoxConfigurationRest;
import org.dspace.app.rest.model.DynamicLayoutMetricsConfigurationRest;
import org.dspace.content.DynamicLayoutMetric2BoxPriorityComparator;
import org.dspace.core.Context;
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutBoxTypes;
import org.dspace.layout.DynamicLayoutMetric2Box;
import org.dspace.layout.service.DynamicLayoutMetric2BoxService;
import org.dspace.metrics.CrisItemMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the configurator for metrics layout box
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
@Component
public class DynamicLayoutMetricsBoxConfigurator implements DynamicLayoutBoxConfigurator {

    @Autowired
    private CrisItemMetricsService crisItemMetricsService;

    @Autowired
    private DynamicLayoutMetric2BoxService dynamicLayoutMetric2BoxService;

    @Override
    public boolean support(DynamicLayoutBox box) {
        return StringUtils.equals(box.getType(), DynamicLayoutBoxTypes.METRICS.name());
    }

    @Override
    public DynamicLayoutBoxConfigurationRest getConfiguration(DynamicLayoutBox box) {
        DynamicLayoutMetricsConfigurationRest rest = new DynamicLayoutMetricsConfigurationRest();
        rest.setMaxColumns(box.getMaxColumns());
        List<DynamicLayoutMetric2Box> layoutMetrics = box.getMetric2box();
        Collections.sort(layoutMetrics, new DynamicLayoutMetric2BoxPriorityComparator());
        rest.setMetrics(metricList(layoutMetrics));
        return rest;
    }

    private List<String> metricList(final List<DynamicLayoutMetric2Box> layoutMetrics) {
        final List<String> result = new LinkedList<>();
        layoutMetrics.forEach(lm -> {
            final String type = lm.getType();
            result.add(type);
            crisItemMetricsService
                .embeddableFallback(type)
                .ifPresent(result::add);
        });
        return result;
    }

    @Override
    public void configure(Context context, DynamicLayoutBox box, DynamicLayoutBoxConfigurationRest rest) {
        if (!(rest instanceof DynamicLayoutMetricsConfigurationRest)) {
            throw new IllegalArgumentException("Invalid METRICS configuration provided");
        }

        DynamicLayoutMetricsConfigurationRest metricsConfiguration = ((DynamicLayoutMetricsConfigurationRest) rest);
        box.setMaxColumns(metricsConfiguration.getMaxColumns());
        dynamicLayoutMetric2BoxService.addMetrics(context, box, metricsConfiguration.getMetrics());

    }

}
