/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.CrisLayoutBoxConfigurationRest;
import org.dspace.app.rest.model.CrisLayoutMetricsConfigurationRest;
import org.dspace.content.CrisLayoutMetric2BoxPriorityComparator;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxTypes;
import org.dspace.layout.CrisLayoutMetric2Box;
import org.springframework.stereotype.Component;

/**
 * This is the configurator for metrics layout box
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
@Component
public class CrisLayoutMetricsBoxConfigurator implements CrisLayoutBoxConfigurator {

    @Override
    public boolean support(CrisLayoutBox box) {
        return StringUtils.equals(box.getType(), CrisLayoutBoxTypes.METRICS.name());
    }

    @Override
    public CrisLayoutBoxConfigurationRest getConfiguration(CrisLayoutBox box) {
        CrisLayoutMetricsConfigurationRest rest = new CrisLayoutMetricsConfigurationRest();
        rest.setId(box.getID());
        rest.setMaxColumns(box.getMaxColumns());
        List<CrisLayoutMetric2Box> layoutMetrics = box.getMetric2box();
        Collections.sort(layoutMetrics, new CrisLayoutMetric2BoxPriorityComparator());
        rest.setMetrics(layoutMetrics.stream().map(lm -> lm.getType()).collect(Collectors.toList()));
        return rest;
    }

}
