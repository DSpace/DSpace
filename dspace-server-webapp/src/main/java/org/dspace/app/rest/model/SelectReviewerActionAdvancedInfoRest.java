/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 *  The SelectReviewerActionAdvancedInfoRest REST Resource
 */
public class SelectReviewerActionAdvancedInfoRest extends WorkflowActionRest {

    private String groupId;
    private String type;
    private String id;

    /**
     * Generic getter for the group
     *
     * @return the group value of this SelectReviewerActionAdvancedInfoRest
     */
    public String getGroup() {
        return groupId;
    }

    /**
     * Generic setter for the group uuid
     *
     * @param groupId The group uuid to be set on this SelectReviewerActionAdvancedInfoRest
     */
    public void setGroup(String groupId) {
        this.groupId = groupId;
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
