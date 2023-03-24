/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.contentreport;

import java.util.Arrays;
import java.util.function.BiFunction;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import org.apache.commons.lang3.function.TriFunction;
import org.dspace.content.MetadataValue;
import org.dspace.content.MetadataValue_;
import org.dspace.util.DSpacePostgreSQLDialect;
import org.dspace.util.JpaCriteriaBuilderKit;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StandardBasicTypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Operators available for creating predicates to query the
 * Filtered Items report
 * @author Jean-François Morin (Université Laval)
 */
public enum QueryOperator {

    EXISTS("exists", true, false,
            (val, regexClause) -> Property.forName("mv.value").isNotNull(),
            (val, regexClause, jpaKit) -> jpaKit.criteriaBuilder().isNotNull(jpaKit.root().get(MetadataValue_.VALUE))),
    DOES_NOT_EXIST("doesnt_exist", true, true,
            (val, regexClause) -> EXISTS.buildPredicate(val, regexClause),
            (val, regexClause, jpaKit) -> EXISTS.buildJpaPredicate(val, regexClause, jpaKit)),
    EQUALS("equals", true, false,
            (val, regexClause) -> Property.forName("mv.value").eq(val),
            (val, regexClause, jpaKit) -> jpaKit.criteriaBuilder().equal(jpaKit.root().get(MetadataValue_.VALUE), val)),
    DOES_NOT_EQUAL("not_equals", true, true,
            (val, regexClause) -> EQUALS.buildPredicate(val, regexClause),
            (val, regexClause, jpaKit) -> EQUALS.buildJpaPredicate(val, regexClause, jpaKit)),
    LIKE("like", true, false,
            (val, regexClause) -> Property.forName("mv.value").like(val),
            (val, regexClause, jpaKit) -> jpaKit.criteriaBuilder().like(jpaKit.root().get(MetadataValue_.VALUE), val)),
    NOT_LIKE("not_like", true, true,
            (val, regexClause) -> LIKE.buildPredicate(val, regexClause),
            (val, regexClause, jpaKit) -> LIKE.buildJpaPredicate(val, regexClause, jpaKit)),
    CONTAINS("contains", true, false,
            (val, regexClause) -> Property.forName("mv.value").like("%" + val + "%"),
            (val, regexClause, jpaKit) -> LIKE.buildJpaPredicate("%" + val + "%", regexClause, jpaKit)),
    DOES_NOT_CONTAIN("doesnt_contain", true, true,
            (val, regexClause) -> CONTAINS.buildPredicate(val, regexClause),
            (val, regexClause, jpaKit) -> CONTAINS.buildJpaPredicate(val, regexClause, jpaKit)),
    MATCHES("matches", false, false,
            (val, regexClause) -> Restrictions.sqlRestriction(regexClause, val, StandardBasicTypes.STRING),
            (val, regexClause, jpaKit) -> regexPredicate(val, DSpacePostgreSQLDialect.REGEX_MATCHES, jpaKit)),
    DOES_NOT_MATCH("doesnt_match", false, false,
            (val, regexClause) -> Restrictions.not(Restrictions.sqlRestriction(
                    regexClause, val, StandardBasicTypes.STRING)),
            (val, regexClause, jpaKit) -> regexPredicate(val, DSpacePostgreSQLDialect.REGEX_NOT_MATCHES, jpaKit));

    private String code;
    /** Criteria builder for the old Hibernate API */
    @Deprecated(forRemoval = true)
    private BiFunction<String, String, Criterion> criterionBuilder;
    private TriFunction<String, String, JpaCriteriaBuilderKit<MetadataValue>, Predicate> predicateBuilder;
    private boolean usesRegex;
    private boolean negate;

    QueryOperator(String code, boolean usesRegex, boolean negate,
            BiFunction<String, String, Criterion> criterionBuilder,
            TriFunction<String, String, JpaCriteriaBuilderKit<MetadataValue>, Predicate> predicateBuilder) {
        this.code = code;
        this.usesRegex = usesRegex;
        this.negate = negate;
        this.criterionBuilder = criterionBuilder;
        this.predicateBuilder = predicateBuilder;
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

    public Predicate buildJpaPredicate(String val, String regexClause, JpaCriteriaBuilderKit<MetadataValue> jpaKit) {
        return predicateBuilder.apply(val, regexClause, jpaKit);
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

    private static Predicate regexPredicate(String val, String regexFunction, JpaCriteriaBuilderKit<MetadataValue> jpaKit) {
        // Source: https://stackoverflow.com/questions/24995881/use-regular-expressions-in-jpa-criteriabuilder
        CriteriaBuilder builder = jpaKit.criteriaBuilder();
        Expression<String> patternExpression = builder.<String>literal(val);
        Path<String> path = jpaKit.root().get(MetadataValue_.VALUE);
        // "matches" comes from the name of the regex function
        // defined in class DSpacePostgreSQLDialect
        return builder.equal(builder
                .function(regexFunction, Boolean.class, path, patternExpression), Boolean.TRUE);
    }

}
