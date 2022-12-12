/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.contentreports;

import java.util.Arrays;
import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StandardBasicTypes;

/**
 * Operators available for creating predicates to query the
 * Filtered Items report
 * @author Jean-François Morin (Université Laval)
 */
public enum QueryOperator {

    EXISTS("exists", true, false, (val, regexClause) -> Property.forName("mv.value").isNotNull()),
    DOES_NOT_EXIST("doesnt_exist", true, true, (val, regexClause) -> EXISTS.buildPredicate(val, regexClause)),
    EQUALS("equals", true, false, (val, regexClause) -> Property.forName("mv.value").eq(val)),
    DOES_NOT_EQUAL("not_equals", true, true, (val, regexClause) -> EQUALS.buildPredicate(val, regexClause)),
    LIKE("like", true, false, (val, regexClause) -> Property.forName("mv.value").like(val)),
    NOT_LIKE("not_like", true, true, (val, regexClause) -> LIKE.buildPredicate(val, regexClause)),
    CONTAINS("contains", true, false, (val, regexClause) -> Property.forName("mv.value").like("%" + val + "%")),
    DOES_NOT_CONTAIN("doesnt_contain", true, true, (val, regexClause) -> CONTAINS.buildPredicate(val, regexClause)),
    MATCHES("matches", false, false,
        (val, regexClause) -> Restrictions.sqlRestriction(regexClause, val, StandardBasicTypes.STRING)),
    DOES_NOT_MATCH("doesnt_match", false, true, (val, regexClause) -> MATCHES.buildPredicate(val, regexClause));

    private String code;
    private BiFunction<String, String, Criterion> criterionBuilder;
    private boolean usesRegex;
    private boolean negate;

    QueryOperator(String code, boolean usesRegex, boolean negate,
            BiFunction<String, String, Criterion> criterionBuilder) {
        this.code = code;
        this.usesRegex = usesRegex;
        this.negate = negate;
        this.criterionBuilder = criterionBuilder;
    }

    @JsonProperty
    public String getCode() {
        return code;
    }

    public boolean getUsesRegex() {
        return usesRegex;
    }

    public boolean getNegate() {
        return negate;
    }

    public Criterion buildPredicate(String val, String regexClause) {
        return criterionBuilder.apply(val, regexClause);
    }

    @JsonCreator
    public static QueryOperator get(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    public BiFunction<String, String, Criterion> getCriterionBuilder() {
        return criterionBuilder;
    }

}
