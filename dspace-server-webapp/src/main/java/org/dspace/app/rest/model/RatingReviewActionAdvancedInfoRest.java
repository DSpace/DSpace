/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

public class RatingReviewActionAdvancedInfoRest extends WorkflowActionRest {

    private boolean descriptionRequired;
    private int maxValue;
    private String type;
    private String id;

    /**
     * Generic getter for the description required boolean
     *
     * @return the description required boolean value of this ScoreReviewActionAdvancedInfoRest
     */
    public boolean isDescriptionRequired() {
        return descriptionRequired;
    }

    /**
     * Generic setter for the description required boolean
     *
     * @param descriptionRequired The description required boolean to be set on this ScoreReviewActionAdvancedInfoRest
     */
    public void setDescriptionRequired(boolean descriptionRequired) {
        this.descriptionRequired = descriptionRequired;
    }

    /**
     * Generic getter for the max value
     *
     * @return the max value of this ScoreReviewActionAdvancedInfoRest
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * Generic setter for the max value
     *
     * @param maxValue The max value to be set on this ScoreReviewActionAdvancedInfoRest
     */
    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Generic getter for the type
     *
     * @return the type of this ScoreReviewActionAdvancedInfoRest
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * Generic setter for the type
     *
     * @param type The type to be set on this ScoreReviewActionAdvancedInfoRest
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Generic getter for the id
     *
     * @return the id of this ScoreReviewActionAdvancedInfoRest
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Generic setter for the id
     *
     * @param id The id to be set on this ScoreReviewActionAdvancedInfoRest
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }
}
