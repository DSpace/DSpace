/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class SortOptionRest implements RestModel {
    public static final String NAME = "sort-option";
    public static final String PLURAL_NAME = "sort-options";

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
    public boolean equals(Object object) {
        return (object instanceof SortOptionRest &&
            new EqualsBuilder().append(this.getName(), ((SortOptionRest) object).getName())
                .append(this.getActualName(), ((SortOptionRest) object).getActualName())
                .isEquals());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(actualName)
            .append(name)
            .toHashCode();
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }
}
