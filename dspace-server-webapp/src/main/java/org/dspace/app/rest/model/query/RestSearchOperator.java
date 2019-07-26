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

/**
 *  This enum defines the different operators that can be used with DSpace
 *  They've been given a regex and the string value of the operator that is used within DSpace.
 *  The purpose of this class is so that the front-end can use these specifications to send queries
 *  using just a search String as defined by the regexes instead of having to pass on the String value
 *  of the operator separately.
 */
public enum RestSearchOperator {

    /**
     * The notcontains operator can be used by adding a - (minus) infront of the search query
     * and a * at the end
     * It then becomes -VALUE* to call for a search on the notcontains operator for VALUE
     */
    NOTCONTAINS("-(.+)\\*", "notcontains"),
    /**
     * The notauthority operator can be used by adding a -id: infront of the search query
     * It then becomes -id:VALUE to call for a search on the notauthority operator for VALUE
     */
    NOTAUTHORITY("-id:(.+)", "notauthority"),
    /**
     * The notequals operator can be used by adding a - infront of the search query
     * It then becomes -VALUE to call for a search on the notequals operator for VALUE
     */
    NOTEQUALS("-(.+)", "notequals"),
    /**
     * The contains operator can be used by adding a * behind the search query
     * It then becomes VALUE* to call for a search on the contains operator for VALUE
     */
    CONTAINS("(.+)\\*", "contains"),
    /**
     * The authority operator can be used by adding an id: infront of the search query
     * It then becomes id:VALUE to call for a search on the authority operator for VALUE
     */
    AUTHORITY("id:(.+)", "authority"),
    /**
     * The equals operator is default and will be used if none of the above are matched
     */
    EQUALS("(.+)", "equals");


    private Pattern regex;

    private String dspaceOperator;

    RestSearchOperator(String regex, String dspaceOperator) {
        this.regex = Pattern.compile(regex);
        this.dspaceOperator = dspaceOperator;
    }

    /**
     * This method extracts the value from the query. It effectively removes the operator parts
     * and returns only the VALUE
     * @param query The full query that was used
     * @return      The value that was passed along in the query without the search operator logic
     */
    public String extractValue(String query) {
        Matcher matcher = regex.matcher(query);
        matcher.find();
        return matcher.group(1);
    }

    /**
     * This method will return the correct RestSearchOperator that's bound to the query given in the parameter.
     * This method will check if the query matches the patterns specified above.
     *
     * @param query The query for which we'll return the appropriate RestSearchOperator
     * @return      The RestSearchOperator that matches with the query
     */
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
