/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.processingaction;

import org.dspace.xmlworkflow.state.actions.ActionAdvancedInfo;

public class ScoreReviewActionAdvancedInfo implements ActionAdvancedInfo {
    private boolean descriptionRequired;
    private int maxValue;

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
}
