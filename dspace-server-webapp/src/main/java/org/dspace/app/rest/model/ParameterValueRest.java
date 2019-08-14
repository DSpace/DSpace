/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.apache.commons.lang3.StringUtils;

/**
 * This class serves as a REST representation for a paramater with a value given to the script
 */
public class ParameterValueRest {

    /**
     * The name of the parameter
     */
    private String name;
    /**
     * The value of the parameter
     */
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String toString() {
        String stringToReturn = "";
        stringToReturn += getName();
        if (StringUtils.isNotBlank(getValue())) {
            stringToReturn += " ";
            stringToReturn += getValue();
        }
        return stringToReturn;
    }
}
