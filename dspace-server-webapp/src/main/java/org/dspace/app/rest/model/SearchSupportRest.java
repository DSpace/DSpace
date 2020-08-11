/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.app.rest.DiscoveryRestController;

/**
 * This class' purpose is to give information about the api/discover endpoint
 */
public class SearchSupportRest extends BaseObjectRest<String> {
    public static final String NAME = "discover";
    public static final String CATEGORY = RestModel.DISCOVER;

    public String getCategory() {
        return CATEGORY;
    }

    public String getType() {
        return NAME;
    }

    public Class getController() {
        return DiscoveryRestController.class;
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof SearchSupportRest &&
            new EqualsBuilder().append(this.getCategory(), ((SearchSupportRest) object).getCategory())
                               .append(this.getType(), ((SearchSupportRest) object).getType())
                               .append(this.getController(), ((SearchSupportRest) object).getController())
                               .isEquals());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(this.getCategory())
            .append(this.getType())
            .append(this.getController())
            .toHashCode();
    }
}
