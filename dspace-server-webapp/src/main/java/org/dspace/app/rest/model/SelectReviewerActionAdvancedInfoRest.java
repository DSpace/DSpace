/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.xmlworkflow.Role;

public class SelectReviewerActionAdvancedInfoRest extends WorkflowActionRest {

    private Role role;
    private String type;
    private String id;

    /**
     * Generic getter for the role
     *
     * @return the role value of this SelectReviewerActionAdvancedInfoRest
     */
    public Role getRole() {
        return role;
    }

    /**
     * Generic setter for the role
     *
     * @param role The role to be set on this SelectReviewerActionAdvancedInfoRest
     */
    public void setRole(Role role) {
        this.role = role;
    }

    /**
     * Generic getter for the type
     *
     * @return the type of this SelectReviewerActionAdvancedInfoRest
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * Generic setter for the type
     *
     * @param type The type to be set on this SelectReviewerActionAdvancedInfoRest
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Generic getter for the id
     *
     * @return the id of this SelectReviewerActionAdvancedInfoRest
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Generic setter for the id
     *
     * @param id The id to be set on this SelectReviewerActionAdvancedInfoRest
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }
}
