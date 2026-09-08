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
 * Domain model representing an allowed general system configuration variable that can be used in email templates.
 *
 * <p>DSpace email templates can access a restricted set of system configuration properties
 * via the Velocity {@code config} map, for example:
 * {@code ${config.get('dspace.name')}} or {@code ${config.get('dspace.ui.url')}}.</p>
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
public class SystemConfigVariable {

    private String key;
    private String value;
    private String placeholder;

    /**
     * Default constructor.
     */
    public SystemConfigVariable() {
    }

    /**
     * Parameterized constructor.
     *
     * @param key the configuration property key (e.g. "dspace.name")
     * @param value the resolved value of the configuration property
     * @param placeholder the Velocity expression to insert into the template
     */
    public SystemConfigVariable(String key, String value, String placeholder) {
        this.key = key;
        this.value = value;
        this.placeholder = placeholder;
    }

    /**
     * Gets the configuration property key (e.g. "dspace.name").
     *
     * @return the property key
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the configuration property key.
     *
     * @param key the property key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the resolved value of the configuration property.
     *
     * @return the property value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the resolved value of the configuration property.
     *
     * @param value the property value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the Velocity expression used to reference this variable in a template
     * (e.g. {@code ${config.get('dspace.name')}}).
     *
     * @return the Velocity placeholder
     */
    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * Sets the Velocity expression used to reference this variable in a template.
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
        SystemConfigVariable that = (SystemConfigVariable) o;
        return Objects.equals(key, that.key) &&
            Objects.equals(value, that.value) &&
            Objects.equals(placeholder, that.placeholder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, placeholder);
    }

    @Override
    public String toString() {
        return "SystemConfigVariable{" +
            "key='" + key + '\'' +
            ", value='" + value + '\'' +
            ", placeholder='" + placeholder + '\'' +
            '}';
    }
}
