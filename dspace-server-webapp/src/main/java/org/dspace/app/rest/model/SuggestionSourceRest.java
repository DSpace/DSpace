/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import org.dspace.app.rest.RestResourceController;

/**
 * The Suggestion Source REST Resource. A suggestion source is a connector to an
 * external system that provides suggestion for a target object of related
 * objects to be imported in the system.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class SuggestionSourceRest extends BaseObjectRest<String> {
    private static final long serialVersionUID = 1L;
    public static final String CATEGORY = RestAddressableModel.INTEGRATION;
    public static final String NAME = "suggestionsource";
    public static final String PLURAL_NAME = "suggestionsources";

    private int total;

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

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
