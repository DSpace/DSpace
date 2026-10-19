/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.contentreport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.dspace.content.MetadataField;

/**
 * Data structure representing a query predicate used by the Filtered Items report
 * to filter items to retrieve.
 * @author Jean-François Morin (Université Laval)
 */
public class QueryPredicate {

    private List<MetadataField> fields = new ArrayList<>();
    private QueryOperator operator;
    private String value;

    /**
     * Shortcut method that builds a QueryPredicate from a single field, an operator, and a value.
     * @param field Predicate subject
     * @param operator Predicate operator
     * @param value Predicate object
     * @return a QueryPredicate instance built from the provided parameters
     */
    public static QueryPredicate of(MetadataField field, QueryOperator operator, String value) {
        var predicate = new QueryPredicate();
        predicate.fields.add(field);
        predicate.operator = operator;
        predicate.value = value;
        return predicate;
    }

    /**
     * Shortcut method that builds a QueryPredicate from a list of fields, an operator, and a value.
     * @param fields Fields that form the predicate subject
     * @param operator Predicate operator
     * @param value Predicate object
     * @return a QueryPredicate instance built from the provided parameters
     */
    public static QueryPredicate of(Collection<MetadataField> fields, QueryOperator operator, String value) {
        var predicate = new QueryPredicate();
        predicate.fields.addAll(fields);
        predicate.operator = operator;
        predicate.value = value;
        return predicate;
    }

    public List<MetadataField> getFields() {
        return fields;
    }

    public QueryOperator getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

}
