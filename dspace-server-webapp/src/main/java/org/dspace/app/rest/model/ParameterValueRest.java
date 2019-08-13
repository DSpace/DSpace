/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.apache.commons.lang3.StringUtils;

public class ParameterValueRest {

    private String name;
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
