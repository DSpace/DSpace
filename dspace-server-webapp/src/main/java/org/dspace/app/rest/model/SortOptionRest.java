/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;

/**
 * This class serves as a REST representation for the {@link DiscoverySortFieldConfiguration} class.
 */
public class SortOptionRest extends RestAddressableModel {
    public static final String NAME = "sortoption";
    public static final String PLURAL_NAME = "sortoptions";

    //TODO Remove this ignore when the proper actualName gets added through the bean ID
    @JsonIgnore
    private String actualName;
    private String name;
    private String sortOrder;

    public void setActualName(String name) {
        this.actualName = name;
    }

    public String getActualName() {
        return actualName;
    }

    public void setName(String metadata) {
        this.name = metadata;
    }

    public String getName() {
        return name;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
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
    public String getCategory() {
        return null;
    }

    @Override
    public Class getController() {
        return null;
    }
}
