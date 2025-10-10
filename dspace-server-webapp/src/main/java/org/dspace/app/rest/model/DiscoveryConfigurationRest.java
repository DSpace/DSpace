/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;
import org.dspace.discovery.configuration.DiscoveryConfiguration;

/**
 * This class serves as a REST representation for the {@link DiscoveryConfiguration} class.
 */
@LinksRest(links = {
    @LinkRest(name = DiscoveryConfigurationRest.SEARCH_FILTERS, method = "getSearchFilters"),
    @LinkRest(name = DiscoveryConfigurationRest.SORT_OPTIONS, method = "getSortOptions"),
    @LinkRest(name = DiscoveryConfigurationRest.DEFAULT_SORT_OPTION, method = "getDefaultSortOption"),
})
public class DiscoveryConfigurationRest extends BaseObjectRest<String> {
    public static final String NAME = "discoveryconfiguration";
    public static final String PLURAL_NAME = "discoveryconfigurations";
    public static final String CATEGORY = RestModel.DISCOVER;

    public static final String SEARCH_FILTERS = "searchfilters";
    public static final String SORT_OPTIONS = "sortoptions";
    public static final String DEFAULT_SORT_OPTION = "defaultsortoption";

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }
}
