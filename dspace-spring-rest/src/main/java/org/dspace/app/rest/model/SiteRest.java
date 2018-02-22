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

/**
 * The Collection REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class SiteRest extends DSpaceObjectRest {
    public static final String NAME = "site";
    public static final String CATEGORY = RestAddressableModel.CORE;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof SiteRest &&
            new EqualsBuilder().append(this.getCategory(), ((SiteRest) object).getCategory())
                               .append(this.getType(), ((SiteRest) object).getType())
                               .isEquals());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(this.getCategory())
            .append(this.getType())
            .toHashCode();
    }
}
