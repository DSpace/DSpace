/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class DSpaceCommandLineParameter {
    private String name;
    private String value;

    public static String SEPARATOR = "|||";

    public DSpaceCommandLineParameter(String key, String value) {
        this.name = key;

        if (StringUtils.isBlank(value)) {
            this.value = null;
        } else {
            this.value = value;
        }
    }

    public DSpaceCommandLineParameter(String parameter) {
        this(StringUtils.substringBefore(parameter, " "), StringUtils.substringAfter(parameter, " "));
    }

    public String getName() {
        return name;
    }

    public void setName(String key) {
        this.name = key;
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

    public static String concatenate(List<DSpaceCommandLineParameter> parameterList) {
        if (parameterList.isEmpty()) {
            return null;
        }
        return parameterList.stream().map(parameter -> parameter.toString()).collect(Collectors.joining(SEPARATOR));
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other.getClass() != DSpaceCommandLineParameter.class) {
            return false;
        }
        return StringUtils.equals(this.getName(), ((DSpaceCommandLineParameter) other).getName()) && StringUtils
            .equals(this.getValue(), ((DSpaceCommandLineParameter) other).getValue());
    }
}
