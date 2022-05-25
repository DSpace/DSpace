/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.utils.parameter;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * Representation for a Discovery search filter
 */
public class QueryBuilderSearchFilter {

    private String name;
    private String operator;
    private String value;

    public QueryBuilderSearchFilter(final String name, final String operator, final String value) {
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

    public String toString() {
        return "QueryBuilderSearchFilter{" +
                "name='" + name + '\'' +
                ", operator='" + operator + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    public boolean equals(Object object) {
        if (object instanceof QueryBuilderSearchFilter) {
            QueryBuilderSearchFilter obj = (QueryBuilderSearchFilter) object;

            if (!StringUtils.equals(obj.getName(), getName())) {
                return false;
            }
            if (!StringUtils.equals(obj.getOperator(), getOperator())) {
                return false;
            }
            if (!StringUtils.equals(obj.getValue(), getValue())) {
                return false;
            }
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(name, operator, value);
    }
}
