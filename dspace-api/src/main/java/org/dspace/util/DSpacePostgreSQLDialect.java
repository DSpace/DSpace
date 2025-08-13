/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;

/**
 * PostgreSQL-specific dialect that adds regular expression support as a JPA function.
 * @see org.dspace.contentreport.QueryOperator
 * @author Jean-François Morin (Université Laval)
 */
public class DSpacePostgreSQLDialect extends PostgreSQLDialect {

    public static final String REGEX_MATCHES = "matches";
    public static final String REGEX_IMATCHES = "imatches";
    public static final String REGEX_NOT_MATCHES = "not_matches";
    public static final String REGEX_NOT_IMATCHES = "not_imatches";

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);

        BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();

        functionContributions.getFunctionRegistry().registerPattern(
            REGEX_MATCHES,
            "?1 ~ ?2",
            basicTypeRegistry.resolve( StandardBasicTypes.BOOLEAN ));
        functionContributions.getFunctionRegistry().registerPattern(
            REGEX_IMATCHES,
            "?1 ~* ?2",
            basicTypeRegistry.resolve( StandardBasicTypes.BOOLEAN ));
        functionContributions.getFunctionRegistry().registerPattern(
            REGEX_NOT_MATCHES,
            "?1 !~ ?2",
            basicTypeRegistry.resolve( StandardBasicTypes.BOOLEAN ));
        functionContributions.getFunctionRegistry().registerPattern(
            REGEX_NOT_IMATCHES,
            "?1 !~* ?2",
            basicTypeRegistry.resolve( StandardBasicTypes.BOOLEAN ));
    }
}
