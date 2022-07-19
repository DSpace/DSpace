/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.model;

import org.apache.commons.lang3.StringUtils;

/**
 * JSON model for a duplicate descision type
 *
 * @author 4Science
 */
public enum DuplicateDecisionType {

    WORKSPACE("WORKSPACE"), WORKFLOW("WORKFLOW"), ADMIN("ADMIN");

    // Type text, to resolve to a flag
    private final String text;

    /**
     * Constructor
     * @param text  decision type
     */
    DuplicateDecisionType(String text) {
        this.text = text;
    }

    /**
     * Render this decision type as a string (eg WORKFLOW)
     * @return  string representation
     */
    @Override
    public String toString() {
        return this.text;
    }

    public static DuplicateDecisionType fromString(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        for (DuplicateDecisionType b : DuplicateDecisionType.values()) {
            if (StringUtils.equals(text, b.text)) {
                return b;
            }
        }
        throw new IllegalArgumentException("No Decision enum with type " + text + " found");
    }
}