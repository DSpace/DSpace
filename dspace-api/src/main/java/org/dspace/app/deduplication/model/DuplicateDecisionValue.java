/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deduplication.model;

public enum DuplicateDecisionValue {

    REJECT("reject"), VERIFY("verify");

    private String text;

    DuplicateDecisionValue(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }

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