/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of supported MIME types for DSpace email templates.
 *
 * <p>Supported formats are {@code text/plain} (default) and {@code text/html}.</p>
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
public enum EmailTemplateMimeType {

    TEXT_PLAIN("text/plain"),
    TEXT_HTML("text/html");

    private final String value;

    EmailTemplateMimeType(String value) {
        this.value = value;
    }

    /**
     * Gets the MIME type string value (e.g. "text/plain" or "text/html").
     *
     * @return the MIME type string
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Resolves an {@link EmailTemplateMimeType} from its string representation.
     * If the input is null, blank, or unrecognized, it safely defaults to {@link #TEXT_PLAIN}.
     *
     * @param text the MIME type string to parse
     * @return matching EmailTemplateMimeType, or TEXT_PLAIN if unrecognized
     */
    @JsonCreator
    public static EmailTemplateMimeType fromString(String text) {
        if (text != null) {
            for (EmailTemplateMimeType type : EmailTemplateMimeType.values()) {
                if (type.value.equalsIgnoreCase(text.trim())) {
                    return type;
                }
            }
        }
        return TEXT_PLAIN;
    }

    @Override
    public String toString() {
        return value;
    }
}
