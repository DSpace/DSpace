/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions;

public interface ActionAdvancedInfo {
    boolean isDescriptionRequired();
    void setDescriptionRequired(boolean descriptionRequired);
    int getMaxValue();
    void setMaxValue(int maxValue);
}
