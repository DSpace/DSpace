/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * This class serves as a REST representation of a Script
 */
public class ScriptRest extends BaseObjectRest<String> {

    public static final String NAME = "script";
    public static final String PLURAL_NAME = "scripts";
    public static final String CATEGORY = RestAddressableModel.SYSTEM;

    private String name;
    private String description;
    @JsonProperty(value = "parameters")
    private List<ParameterRest> parameterRestList = new LinkedList<>();

    public String getCategory() {
        return CATEGORY;
    }

    public Class getController() {
        return RestResourceController.class;
    }

    public String getType() {
        return NAME;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ParameterRest> getParameterRestList() {
        return parameterRestList;
    }

    public void setParameterRestList(List<ParameterRest> parameterRestList) {
        this.parameterRestList = parameterRestList;
    }

    public void addToParameterRestList(ParameterRest parameter) {
        parameterRestList.add(parameter);
    }

}
