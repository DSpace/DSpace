/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.query;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum RestSearchOperator {
    NOTCONTAINS("-(.+)\\*", "notcontains"),
    NOTAUTHORITY("-id:(.+)", "notauthority"),
    NOTEQUALS("-(.+)", "notequals"),
    CONTAINS("(.+)\\*", "contains"),
    AUTHORITY("id:(.+)", "authority"),
    EQUALS("(.+)", "equals");


    private Pattern regex;

    private String dspaceOperator;

    RestSearchOperator(String regex, String dspaceOperator) {
        this.regex = Pattern.compile(regex);
        this.dspaceOperator = dspaceOperator;
    }

    public String extractValue(String query) {
        Matcher matcher = regex.matcher(query);
        matcher.find();
        return matcher.group(1);
    }

    public static RestSearchOperator forQuery(String query) {
        for (RestSearchOperator op : RestSearchOperator.values()) {
            if (op.getRegex().matcher(query).matches()) {
                return op;
            }
        }
        return null;
    }

    public Pattern getRegex() {
        return regex;
    }

    public String getDspaceOperator() {
        return dspaceOperator;
    }
}
