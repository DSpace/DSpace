/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import org.dspace.app.rest.RestResourceController;

/**
 * The Authorization Feature REST Resource, shortly feature. A feature is the representation of a business goal used in
 * the Authorization endpoint to declare what an user can do on a specific object.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class AuthorizationFeatureRest extends BaseObjectRest<String> {
    public static final String NAME = "feature";
    public static final String PLURAL_NAME = "features";
    public static final String CATEGORY = RestAddressableModel.AUTHORIZATION;

    private String description;

    @JsonProperty(value = "resourcetypes")
    private List<String> resourceTypes;

    @Override
    @JsonProperty(access = Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(List<String> resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

}
