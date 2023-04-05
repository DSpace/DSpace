package org.dspace.app.rest;

/**
 * DTO object to pass Wufoo feedback form information to the front-end.
 */
public class WufooFeedbackDTO {

    private String wufooFeedbackFormUrl;

    /**
     * Returns the URL-encoded Wufoo feedback form URL, with
     * "URL modifications" (i.e., default form values) as query parameters.
     */
    public String getWufooFeedbackFormUrl() {
        return wufooFeedbackFormUrl;
    }

    /**
     * Sets the Wufoo feedback form URL
     *
     * @param wufooFeedbackFormUrl the Wufoo feedback form URL to set.
     */
    public void setWufooFormUrl(String wufooFeedbackFormUrl) {
        this.wufooFeedbackFormUrl = wufooFeedbackFormUrl;
    }

    @Override
    public String toString() {
        return "WufooFeedbackDTO [wufooFeedbackFormUrl='" + wufooFeedbackFormUrl + "']";
    }
}
