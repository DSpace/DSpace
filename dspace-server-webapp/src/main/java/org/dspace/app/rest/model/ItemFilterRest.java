/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * The ItemFilter REST Resource
 *
 * @author mohamed eskander (mohamed.eskander at 4science.com)
 */
public class ItemFilterRest extends BaseObjectRest<String> {
    public static final String NAME = "itemfilter";
    public static final String PLURAL_NAME = "itemfilters";
    public static final String CATEGORY = RestAddressableModel.CONFIGURATION;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }
}
