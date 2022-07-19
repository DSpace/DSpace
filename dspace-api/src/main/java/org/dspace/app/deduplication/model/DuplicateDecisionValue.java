/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.model;

/***
 * Define an enumeration to list the choices that can be done to resolve a
 * duplication.
 * <p>
 * Use VERIFY to mark the deduplication to be verified. Use REJECT to mark the
 * deduplication to be ignored.
 *
 * @author 4Science
 */
public enum DuplicateDecisionValue {

    REJECT("reject"), VERIFY("verify");

    // Value text
    private final String text;

    /**
     * Constructor
     * @param text  value text
     */
    DuplicateDecisionValue(String text) {
        this.text = text;
    }

    /**
     * Render this decision value as text
     * @return  string representation
     */
    @Override
    public String toString() {
        return this.text;
    }

    /**
     * Parse a decision value from a string
     * @param text  string to parse
     * @return parsed DuplicateDecisionValue object
     */
    public static DuplicateDecisionValue fromString(String text) {
        if (text == null) {
            return null;
        }
        for (DuplicateDecisionValue b : DuplicateDecisionValue.values()) {
            if (b.text.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new IllegalArgumentException("No visibility enum with text " + text + " found");
    }
}