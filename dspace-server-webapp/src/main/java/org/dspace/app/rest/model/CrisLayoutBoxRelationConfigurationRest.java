/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutBoxRelationConfigurationRest extends BaseObjectRest<Integer>
        implements CrisLayoutBoxConfigurationRest {

    private static final long serialVersionUID = 1L;

    public static final String NAME = "boxrelationconfiguration";
    public static final String CATEGORY = RestAddressableModel.LAYOUT;

    @JsonProperty(value = "discovery-configuration")
    private String discoveryConfiguration;

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.app.rest.model.RestModel#getType()
     */
    @Override
    public String getType() {
        return NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.app.rest.model.RestAddressableModel#getCategory()
     */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dspace.app.rest.model.RestAddressableModel#getController()
     */
    @Override
    public Class<RestResourceController> getController() {
        return RestResourceController.class;
    }

    public String getDiscoveryConfiguration() {
        return discoveryConfiguration;
    }

    public void setDiscoveryConfiguration(String configuration) {
        this.discoveryConfiguration = configuration;
    }
}
