/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;


/**
 * The DynamicLayoutMetricsConfiguration details
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class DynamicLayoutMetricsConfigurationRest implements DynamicLayoutBoxConfigurationRest {

    public static final String NAME = "boxmetricsconfiguration";

    private Integer maxColumns;

    private List<String> metrics = new ArrayList<>();

    private String type = NAME;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<String> metrics) {
        this.metrics = metrics;
    }

    public Integer getMaxColumns() {
        return maxColumns;
    }

    public void setMaxColumns(Integer maxColumns) {
        this.maxColumns = maxColumns;
    }

}
