/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import org.hibernate.dialect.PostgreSQL94Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

/**
 * PostgreSQL-specific dialect that adds regular expression support as a JPA function.
 * @see org.dspace.contentreport.QueryOperator
 * @author Jean-François Morin (Université Laval)
 */
public class DSpacePostgreSQLDialect extends PostgreSQL94Dialect {

    public static final String REGEX_MATCHES = "matches";
    public static final String REGEX_IMATCHES = "imatches";
    public static final String REGEX_NOT_MATCHES = "not_matches";
    public static final String REGEX_NOT_IMATCHES = "not_imatches";

    public DSpacePostgreSQLDialect() {
        registerFunction(REGEX_MATCHES, new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1 ~ ?2"));
        registerFunction(REGEX_IMATCHES, new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1 ~* ?2"));
        registerFunction(REGEX_NOT_MATCHES, new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1 !~ ?2"));
        registerFunction(REGEX_NOT_IMATCHES, new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1 !~* ?2"));
    }

}
