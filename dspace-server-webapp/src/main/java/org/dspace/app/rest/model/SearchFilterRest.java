/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.discovery.configuration.DiscoverySearchFilter;

public class SearchFilterRest {

    private String filter;
    private boolean hasFacets = false;
    private String type;
    private boolean isOpenByDefault = false;
    private List<SearchFilterRest.Operator> operators = new LinkedList<>();
    private int pageSize;

    public static final String OPERATOR_EQUALS = "equals";
    public static final String OPERATOR_NOTEQUALS = "notequals";
    public static final String OPERATOR_AUTHORITY = "authority";
    public static final String OPERATOR_NOTAUTHORITY = "notauthority";
    public static final String OPERATOR_CONTAINS = "contains";
    public static final String OPERATOR_NOTCONTAINS = "notcontains";
    public static final String OPERATOR_QUERY = "query";

    /**
     * Specifies whether this filter has facets or not
     * @return  A boolean indicating whether this filter has facets or not
     */
    public boolean isHasFacets() {
        return hasFacets;
    }

    /**
     * Sets the hasFacets property of the filter class to the given boolean
     *
     * @param hasFacets The boolean that the hasFacets property will be set to
     */
    public void setHasFacets(boolean hasFacets) {
        this.hasFacets = hasFacets;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * This is the same type as described in {@link DiscoverySearchFilter#getType()}
     * @return  The type of this filter
     */
    public String getType() {
        return type;
    }

    /**
     * This is the same type as described in {@link org.dspace.discovery.configuration.DiscoverySearchFilter#setType(String)}
     *
     * @param type  The type for this Filter to be set to
     */
    public void setType(String type) {
        this.type = type;
    }
    /**
     * See documentantion at {@link DiscoverySearchFilter#isOpenByDefault()}
     */
    public boolean isOpenByDefault() {
        return isOpenByDefault;
    }
    /**
     * See documentantion at {@link DiscoverySearchFilter#setIsOpenByDefault(boolean)}
     */
    public void setOpenByDefault(boolean openByDefault) {
        isOpenByDefault = openByDefault;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getFilter() {
        return filter;
    }

    public void addOperator(SearchFilterRest.Operator operator) {
        operators.add(operator);
    }

    public List<SearchFilterRest.Operator> getOperators() {
        return operators;
    }

    public void addDefaultOperatorsToList() {
        operators.add(new SearchFilterRest.Operator(OPERATOR_EQUALS));
        operators.add(new SearchFilterRest.Operator(OPERATOR_NOTEQUALS));
        operators.add(new SearchFilterRest.Operator(OPERATOR_AUTHORITY));
        operators.add(new SearchFilterRest.Operator(OPERATOR_NOTAUTHORITY));
        operators.add(new SearchFilterRest.Operator(OPERATOR_CONTAINS));
        operators.add(new SearchFilterRest.Operator(OPERATOR_NOTCONTAINS));
        operators.add(new SearchFilterRest.Operator(OPERATOR_QUERY));
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof SearchFilterRest &&
            new EqualsBuilder().append(this.filter, ((SearchFilterRest) object).filter)
                .append(this.getOperators(), ((SearchFilterRest) object).getOperators())
                .isEquals());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(filter)
            .append(operators)
            .toHashCode();
    }

    public static class Operator {
        private String operator;

        public Operator(String operator) {
            this.operator = operator;
        }

        public String getOperator() {
            return operator;
        }

        @Override
        public boolean equals(Object object) {
            return (object instanceof SearchFilterRest.Operator &&
                new EqualsBuilder()
                    .append(this.getOperator(), ((SearchFilterRest.Operator) object).getOperator()).isEquals());
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                .append(operator)
                .toHashCode();
        }
    }
}
