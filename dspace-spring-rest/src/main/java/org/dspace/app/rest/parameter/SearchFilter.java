package org.dspace.app.rest.parameter;

/**
 * Custom request parameter used in the Discovery search REST endpoint.
 * This parameter is resolved by TODO
 */
public class SearchFilter {

    private String name;
    private String operator;
    private String value;

    public SearchFilter(final String name, final String operator, final String value) {
        this.name = name;
        this.operator = operator;
        this.value = value;
    }
}
