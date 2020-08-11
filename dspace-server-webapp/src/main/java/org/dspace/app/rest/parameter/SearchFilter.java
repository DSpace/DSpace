/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.parameter;

import org.apache.commons.lang3.StringUtils;

/**
 * Custom request parameter used in the Discovery search REST endpoint.
 * This parameter is resolved by TODO
 */
public class SearchFilter {

    private String name;
    private String operator;
    private String value;

    public SearchFilter(final String name, final String operator, final String value) {
        this.name = name;
        this.operator = operator;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public boolean hasAuthorityOperator() {
        return StringUtils.equals(operator, "authority");
    }

    public String toString() {
        return "SearchFilter{" +
            "name='" + name + '\'' +
            ", operator='" + operator + '\'' +
            ", value='" + value + '\'' +
            '}';
    }
}
