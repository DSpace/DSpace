/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 * Rest object used to represent a plain  text value
 */
public class PlainTextValueRest {
    public static final String TYPE = "plaintextvalue";

    private String value;

    public PlainTextValueRest() {
    }

    public PlainTextValueRest(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public String getType() {
        return TYPE;
    }
}
