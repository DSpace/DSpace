/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 * This class serves as a REST representation for a script parameter
 */
public class ParameterRest {

    /**
     * The name of the parameter
     */
    private String name;
    /**
     * The description of the parameter
     */
    private String description;
    /**
     * The type of the parameter
     */
    private String type;

    /**
     * Boolean indicating whether the parameter is mandatory or not
     */
    private boolean mandatory;

    /**
     * The long name of the parameter
     */
    private String nameLong;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * Generic getter for the nameLong
     * @return the nameLong value of this ParameterRest
     */
    public String getNameLong() {
        return nameLong;
    }

    /**
     * Generic setter for the nameLong
     * @param nameLong   The nameLong to be set on this ParameterRest
     */
    public void setNameLong(String nameLong) {
        this.nameLong = nameLong;
    }

    /**
     * Generic getter for the mandatory
     * @return the mandatory value of this ParameterRest
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * Generic setter for the mandatory
     * @param mandatory   The mandatory to be set on this ParameterRest
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }
}
