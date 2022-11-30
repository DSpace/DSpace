/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.contentreports.QueryOperator;

public class FilteredItemsQueryPredicate {

    private String field;
    private QueryOperator operator;
    private String value;

    public static FilteredItemsQueryPredicate of(String field, QueryOperator operator, String value) {
        var predicate = new FilteredItemsQueryPredicate();
        predicate.field = field;
        predicate.operator = operator;
        predicate.value = value;
        return predicate;
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

}
