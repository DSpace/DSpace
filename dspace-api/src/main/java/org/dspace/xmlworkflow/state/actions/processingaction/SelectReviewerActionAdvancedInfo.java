/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import org.dspace.xmlworkflow.Role;
import org.dspace.xmlworkflow.state.actions.ActionAdvancedInfo;
import org.springframework.util.DigestUtils;

public class SelectReviewerActionAdvancedInfo implements ActionAdvancedInfo {
    private Role role;
    private String type;
    private String id;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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
            + ";role," + role;
        this.id = DigestUtils.md5DigestAsHex(idString.getBytes());
    }
}

