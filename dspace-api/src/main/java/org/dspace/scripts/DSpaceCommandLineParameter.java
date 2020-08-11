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
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * This class serves as a representation of a command line parameter by holding a String name and a String value
 */
public class DSpaceCommandLineParameter {
    private String name;
    private String value;

    public static String SEPARATOR = "|||";

    /**
     * This constructor will take a String key and String value and store them in their appriopriate fields
     * @param key   The String value to be stored as the name of the parameter
     * @param value The String value to be stored as the value of the parameter
     */
    public DSpaceCommandLineParameter(String key, String value) {
        this.name = key;

        if (StringUtils.isBlank(value)) {
            this.value = null;
        } else {
            this.value = value;
        }
    }

    /**
     * This constructors accepts a single parameter String that is defined as e.g. "-c test" and it'll parse this
     * String into the key "-c" and value "test" to then call the other constructor with those parameters
     * @param parameter The String parameter
     */
    protected DSpaceCommandLineParameter(String parameter) {
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

    /**
     * Converts the DSpaceCommandLineParameter into a String format by concatenating the value and the name String
     * values by separating them with a space
     * @return The String representation of a DSpaceCommandlineParameter object
     */
    public String toString() {
        String stringToReturn = "";
        stringToReturn += getName();
        if (StringUtils.isNotBlank(getValue())) {
            stringToReturn += " ";
            stringToReturn += getValue();
        }
        return stringToReturn;
    }

    /**
     * This method will convert a list of DSpaceCommandLineParameter objects into a single String. This is done by
     * calling the toString() method on each of the DSpaceCommandLineParameter objects in the list and concatenating
     * them with the Separator defined in this class
     * @param parameterList The list of DSpaceCommandLineParameter objects to be converted into a String
     * @return              The resulting String
     */
    public static String concatenate(List<DSpaceCommandLineParameter> parameterList) {
        if (parameterList.isEmpty()) {
            return null;
        }
        return parameterList.stream().map(parameter -> parameter.toString()).collect(Collectors.joining(SEPARATOR));
    }

    /**
     * Will return a boolean indicating whether the given param is equal to this object
     * @param other The other object
     * @return      A boolean indicating equality
     */
    @Override
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder(5, 17)
            .append(this.getName())
            .append(this.getValue())
            .toHashCode();
    }
}
