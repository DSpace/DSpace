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
 * See config {@code workflow-actions.cfg}
 */
public class SelectReviewerActionAdvancedInfo extends ActionAdvancedInfo {
    private String group;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public void generateId(String type) {
        String idString = type
            + ";group," + group;
        super.id = DigestUtils.md5DigestAsHex(idString.getBytes());
    }
}

