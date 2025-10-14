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

import org.dspace.discovery.configuration.DiscoverySearchFilter;

/**
 * This class serves as a REST representation for the {@link DiscoverySearchFilter} class.
 */
public class SearchFilterRest extends RestAddressableModel {
    public static final String NAME = "searchfilter";
    public static final String PLURAL_NAME = "searchfilters";

    private String filter;
    private boolean hasFacets = false;
    private String filterType;
    private boolean isOpenByDefault = false;
    private int pageSize;
    private List<SearchFilterRest.Operator> operators = new LinkedList<>();
    private String type;

    public static final String OPERATOR_EQUALS = "equals";
    public static final String OPERATOR_NOTEQUALS = "notequals";
    public static final String OPERATOR_AUTHORITY = "authority";
    public static final String OPERATOR_NOTAUTHORITY = "notauthority";
    public static final String OPERATOR_CONTAINS = "contains";
    public static final String OPERATOR_NOTCONTAINS = "notcontains";
    public static final String OPERATOR_QUERY = "query";

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public boolean isHasFacets() {
        return hasFacets;
    }

    public void setHasFacets(boolean hasFacets) {
        this.hasFacets = hasFacets;
    }

    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public boolean isOpenByDefault() {
        return isOpenByDefault;
    }

    public void setOpenByDefault(boolean openByDefault) {
        isOpenByDefault = openByDefault;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
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
    public String getCategory() {
        return null;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public Class getController() {
        return null;
    }

    public static class Operator {
        private String operator;

        public Operator(String operator) {
            this.operator = operator;
        }

        public String getOperator() {
            return operator;
        }
    }
}
