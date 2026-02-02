/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.dspace.contentreport.QueryOperator;

/**
 * Data structure representing a query predicate used by the Filtered Items report
 * to filter items to retrieve. This version is specific to the REST layer and its
 * property types are detached from the persistence layer.
 * @see org.dspace.contentreport.QueryPredicate
 * @author Jean-François Morin (Université Laval)
 */
public class FilteredItemsQueryPredicate {

    private String field;
    private QueryOperator operator;
    private String value;

    /**
     * Shortcut method that builds a FilteredItemsQueryPredicate from a single field, an operator, and a value.
     * @param field Predicate subject
     * @param operator Predicate operator
     * @param value Predicate object
     * @return a FilteredItemsQueryPredicate instance built from the provided parameters
     */
    public static FilteredItemsQueryPredicate of(String field, QueryOperator operator, String value) {
        var predicate = new FilteredItemsQueryPredicate();
        predicate.field = field;
        predicate.operator = operator;
        predicate.value = value;
        return predicate;
    }

    /**
     * Shortcut method that builds a FilteredItemsQueryPredicate from a colon-separated string value.
     * @param value Colon-separated string value (field:operator:object or field:operator)
     * @return a FilteredItemsQueryPredicate instance built from the provided value
     */
    public static FilteredItemsQueryPredicate of(String value) {
        String[] tokens = value.split("\\:");
        String field = tokens.length > 0 ? tokens[0].trim() : "";
        QueryOperator operator = tokens.length > 1 ? QueryOperator.get(tokens[1].trim()) : null;
        String object = tokens.length > 2 ? StringUtils.trimToEmpty(tokens[2]) : "";
        return of(field, operator, object);
    }

    public String getField() {
        return field;
    }

    public QueryOperator getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        String op = Optional.ofNullable(operator).map(QueryOperator::getCode).orElse("");
        return field + ":" + op + ":" + value;
    }

}
