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

import org.dspace.app.rest.RestResourceController;

public class ProcessFileTypesRest extends BaseObjectRest<String> {

    public static final String NAME = "filetypes";
    public static final String PLURAL_NAME = "filetypes";
    public static final String CATEGORY = RestAddressableModel.SYSTEM;

    private List<String> values;

    /**
     * Generic getter for the values
     * @return the values value of this ProcessFileTypesRest
     */
    public List<String> getValues() {
        return values;
    }

    /**
     * Generic setter for the values
     * @param values   The values to be set on this ProcessFileTypesRest
     */
    public void setValues(List<String> values) {
        this.values = values;
    }

    public void addValue(String value) {
        if (values == null) {
            values = new LinkedList<>();
        }
        values.add(value);
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
}
