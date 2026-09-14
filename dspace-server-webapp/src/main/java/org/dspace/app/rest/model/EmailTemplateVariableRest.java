/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.io.Serializable;

import jakarta.validation.constraints.Size;

/**
 * REST representation of a variable (parameter) in an email template.
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
public class EmailTemplateVariableRest implements Serializable {

    private int index;

    @Size(max = 500)
    private String description;

    @Size(max = 30)
    private String placeholder;

    /**
     * Default constructor.
     */
    public EmailTemplateVariableRest() {
    }

    /**
     * Constructs an EmailTemplateVariableRest with all fields.
     *
     * @param index       the parameter index
     * @param description the description
     * @param placeholder the VTL placeholder string
     */
    public EmailTemplateVariableRest(int index, String description, String placeholder) {
        this.index = index;
        this.description = description;
        this.placeholder = placeholder;
    }

    /**
     * Gets the 0-based parameter index.
     *
     * @return the parameter index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Sets the 0-based parameter index.
     *
     * @param index the parameter index to set
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Gets the parameter description.
     *
     * @return the parameter description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the parameter description.
     *
     * @param description the parameter description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the VTL placeholder string (e.g. {@code ${params[0]}}).
     *
     * @return the VTL placeholder
     */
    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * Sets the VTL placeholder string.
     *
     * @param placeholder the VTL placeholder to set
     */
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }
}
