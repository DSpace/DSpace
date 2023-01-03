/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import org.dspace.xmlworkflow.state.actions.ActionAdvancedInfo;
import org.springframework.util.DigestUtils;

/**
 * Class that holds the advanced information needed for the
 * {@link org.dspace.xmlworkflow.state.actions.processingaction.SelectReviewerAction}
 */
public class SelectReviewerActionAdvancedInfo implements ActionAdvancedInfo {
    private String group;
    private String type;
    private String id;

    /**
     * Generic getter for the group
     *
     * @return the group value
     */
    public String getGroup() {
        return group;
    }

    /**
     * Generic setter for the group
     *
     * @param group The group to be set
     */
    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = "action_info_" + type;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String type) {
        String idString = type
            + ";group," + group;
        this.id = DigestUtils.md5DigestAsHex(idString.getBytes());
    }
}

