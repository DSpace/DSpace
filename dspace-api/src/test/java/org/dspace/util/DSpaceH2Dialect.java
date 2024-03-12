/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

/**
 * H2-specific dialect that adds regular expression support as a function.
 * @author Jean-François Morin (Université Laval)
 */
public class DSpaceH2Dialect extends H2Dialect {

    private static Map<String, Pattern> regexCache = new HashMap<>();

    public DSpaceH2Dialect() {
        registerFunction("matches", new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "matches(?1, ?2)"));

        // The SQL function is registered in AbstractIntegrationTestWithDatabase.initDatabase().
    }

    public static boolean matches(String regex, String value) {
        Pattern pattern = regexCache.get(regex);
        if (pattern == null) {
            pattern = Pattern.compile(regex);
            regexCache.put(regex, pattern);
        }
        return pattern.matcher(value).matches();
    }

}
