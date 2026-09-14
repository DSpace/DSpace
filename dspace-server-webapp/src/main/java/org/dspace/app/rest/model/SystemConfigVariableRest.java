/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.dspace.app.rest.RestResourceController;

/**
 * REST representation of an allowed system configuration variable that can be used in email templates.
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
public class SystemConfigVariableRest extends BaseObjectRest<String> {

    public static final String NAME = "systemconfigvariable";
    public static final String PLURAL_NAME = "systemconfigvariables";
    public static final String CATEGORY = RestAddressableModel.SYSTEM;

    @Size(max = 255)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
    private String key;

    @Size(max = 2000)
    private String value;

    @Size(max = 255)
    private String placeholder;

    /**
     * Default constructor.
     */
    public SystemConfigVariableRest() {
    }

    /**
     * Parameterized constructor.
     *
     * @param key the configuration property key (e.g. "dspace.name")
     * @param value the resolved value of the configuration property
     * @param placeholder the Velocity expression to insert into the template
     */
    public SystemConfigVariableRest(String key, String value, String placeholder) {
        this.key = key;
        this.value = value;
        this.placeholder = placeholder;
    }

    /**
     * Gets the REST resource identifier, which is the configuration property key.
     *
     * @return the property key, or the raw id if key is not set
     */
    @Override
    public String getId() {
        return this.key != null ? this.key : this.id;
    }

    /**
     * Sets the REST resource identifier, syncing it with the configuration property key.
     *
     * @param id the property key identifier to set
     */
    @Override
    public void setId(String id) {
        this.id = id;
        this.key = id;
    }

    /**
     * Gets the API category of this resource.
     *
     * @return the system category
     */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    /**
     * Gets the controller class handling this resource.
     *
     * @return the REST resource controller class
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Class getController() {
        return RestResourceController.class;
    }

    /**
     * Gets the resource type name.
     *
     * @return the systemconfigvariable type name
     */
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    /**
     * Gets the plural resource type name.
     *
     * @return the systemconfigvariables plural type name
     */
    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
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
     * Sets the configuration property key, syncing it with the REST id.
     *
     * @param key the property key to set
     */
    public void setKey(String key) {
        this.key = key;
        this.id = key;
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
     * Gets the Velocity expression used to reference this variable in a template.
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
        SystemConfigVariableRest that = (SystemConfigVariableRest) o;
        return Objects.equals(key, that.key) &&
            Objects.equals(value, that.value) &&
            Objects.equals(placeholder, that.placeholder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, placeholder);
    }
}
