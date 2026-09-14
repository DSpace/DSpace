/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.util.Objects;

/**
 * Domain model representing a variable (parameter) in an email template.
 *
 * <p>In DSpace email templates, positional parameters are documented in comments
 * such as:
 * <pre>
 * ## Parameters: {0} a string giving the user's name
 * ##             {1} a string giving the item title
 * </pre>
 * or shorthand:
 * <pre>
 * ## {0} recipient name
 * ## {1} confirmation URL
 * </pre>
 * and are referenced in the template body as {@code ${params[0]}}, {@code ${params[1]}}, etc.
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
public class EmailTemplateVariable {

    private int index;
    private String description;
    private String placeholder;

    /**
     * Default constructor.
     */
    public EmailTemplateVariable() {
    }

    /**
     * Parameterized constructor.
     *
     * @param index       the 0-based parameter index
     * @param description description extracted from the template comment
     * @param placeholder the Velocity expression to insert into the template (e.g. {@code ${params[0]}})
     */
    public EmailTemplateVariable(int index, String description, String placeholder) {
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
     * Gets the parameter description extracted from the template comment header.
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
     * Gets the Velocity expression used to reference this parameter in the template body
     * (e.g. {@code ${params[0]}}).
     *
     * @return the Velocity placeholder
     */
    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * Sets the Velocity expression used to reference this parameter in the template body.
     *
     * @param placeholder the Velocity placeholder to set
     */
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EmailTemplateVariable that = (EmailTemplateVariable) o;
        return index == that.index &&
            Objects.equals(description, that.description) &&
            Objects.equals(placeholder, that.placeholder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, description, placeholder);
    }

    @Override
    public String toString() {
        return "EmailTemplateVariable{" +
            "index=" + index +
            ", description='" + description + '\'' +
            ", placeholder='" + placeholder + '\'' +
            '}';
    }
}
