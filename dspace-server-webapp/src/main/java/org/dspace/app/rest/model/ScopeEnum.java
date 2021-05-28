/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The possible restriction options for the scope attributes in the
 * SubmissionPanel resource and SubmissionForm's fields
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public enum ScopeEnum {

    @JsonProperty("submission")
    SUBMISSION("submission"),

    @JsonProperty("workflow")
    WORKFLOW("workflow"),

    @JsonProperty("edit")
    EDIT("workflow");

    private String text;

    ScopeEnum(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return this.text;
    }

    public static ScopeEnum fromString(String text) {
        if (text == null) {
            return null;
        }
        for (ScopeEnum b : ScopeEnum.values()) {
            if (b.text.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new IllegalArgumentException("No scope enum with text " + text + " found");
    }

}
