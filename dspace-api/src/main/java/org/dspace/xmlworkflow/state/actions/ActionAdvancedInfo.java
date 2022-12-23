/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions;

public interface ActionAdvancedInfo {
    /**
     * Getter for the type of Action
     *
     * @return String of format "action_info_" + Action type
     */
    String getType();

    /**
     * Setter for the Action type to be set in format "action_info_" + type
     *
     * @param type The type to be set
     */
    void setType(String type);

    /**
     * Getter for the id of Action
     *
     * @return MD5 hash String of this Action
     */
    String getId();

    /**
     * Setter for the Action id to be set.
     * This is an MD5 hash of the type and the stringified properties of the advanced info
     *
     * @param type The type of this Action to be included in the MD5 hash
     */
    void setId(String type);

}
