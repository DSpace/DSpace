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
 * {@link org.dspace.xmlworkflow.state.actions.processingaction.RatingReviewAction}
 */
public class RatingReviewActionAdvancedInfo implements ActionAdvancedInfo {
    private boolean descriptionRequired;
    private int maxValue;
    private String type;
    private String id;


    /**
     * Generic getter for the descriptionRequired boolean
     *
     * @return the descriptionRequired boolean value
     */
    public boolean isDescriptionRequired() {
        return descriptionRequired;
    }

    /**
     * Generic setter for the descriptionRequired boolean
     *
     * @param descriptionRequired The descriptionRequired boolean to be set
     */
    public void setDescriptionRequired(boolean descriptionRequired) {
        this.descriptionRequired = descriptionRequired;
    }

    /**
     * Generic getter for the maxValue
     *
     * @return the maxValue value
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * Generic setter for the maxValue
     *
     * @param maxValue The maxValue to be set
     */
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
