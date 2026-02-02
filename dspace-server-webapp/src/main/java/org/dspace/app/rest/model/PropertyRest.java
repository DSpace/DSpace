/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RestResourceController;

/**
 * This class acts as the REST representation of a DSpace configuration property.
 * This class acts as a data holder for the PropertyResource
 */
public class PropertyRest extends BaseObjectRest<String> {
    public static final String NAME = "property";
    public static final String PLURAL_NAME = "properties";
    public static final String CATEGORY = RestAddressableModel.CONFIGURATION;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public String name;
    public List<String> values;

    @Override
    @JsonIgnore
    public String getId() {
        return this.name;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }
}
