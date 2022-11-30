/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.contentreports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.dspace.content.MetadataField;

public class QueryPredicate {

    private List<MetadataField> fields = new ArrayList<>();
    private QueryOperator operator;
    private String value;

    public static QueryPredicate of(MetadataField field, QueryOperator operator, String value) {
        var predicate = new QueryPredicate();
        predicate.fields.add(field);
        predicate.operator = operator;
        predicate.value = value;
        return predicate;
    }

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
