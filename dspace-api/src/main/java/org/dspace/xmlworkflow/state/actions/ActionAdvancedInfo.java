/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions;

/**
 * Interface for the shared properties of an 'advancedInfo' section of an advanced workflow {@link Action}
 * Implementations of this class will define the specific fields per action that will need to be defined/configured
 * to pass along this info to REST endpoint
 */
public abstract class ActionAdvancedInfo {

    protected String type;
    protected String id;

    protected final static String TYPE_PREFIX = "action_info_";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = TYPE_PREFIX + type;
    }

    public String getId() {
        return id;
    }

    /**
     * Setter for the Action id to be set.
     * This is an MD5 hash of the type and the stringified properties of the advanced info
     *
     * @param type The type of this Action to be included in the MD5 hash
     */
    protected abstract void generateId(String type);

}
