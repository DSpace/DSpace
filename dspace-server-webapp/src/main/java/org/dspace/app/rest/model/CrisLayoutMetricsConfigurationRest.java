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

import org.dspace.app.rest.RestResourceController;


/**
 * The CrisLayoutMetricsConfiguration details
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class CrisLayoutMetricsConfigurationRest extends BaseObjectRest<Integer>
        implements CrisLayoutBoxConfigurationRest {

    private static final long serialVersionUID = 6507793437056522786L;
    public static final String NAME = "boxmetricsconfiguration";
    public static final String CATEGORY = RestAddressableModel.LAYOUT;

    private Integer maxColumns;

    private List<String> metrics = new ArrayList<>();

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestModel#getType()
     */
    @Override
    public String getType() {
        return NAME;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestAddressableModel#getCategory()
     */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestAddressableModel#getController()
     */
    @Override
    public Class<RestResourceController> getController() {
        return RestResourceController.class;
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
