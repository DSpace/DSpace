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

public class RatingReviewActionAdvancedInfo implements ActionAdvancedInfo {
    private boolean descriptionRequired;
    private int maxValue;
    private String type;
    private String id;


    public boolean isDescriptionRequired() {
        return descriptionRequired;
    }

    public void setDescriptionRequired(boolean descriptionRequired) {
        this.descriptionRequired = descriptionRequired;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
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
            + ";descriptionRequired," + descriptionRequired
            + ";maxValue," + maxValue;
        this.id = DigestUtils.md5DigestAsHex(idString.getBytes());
    }
}
