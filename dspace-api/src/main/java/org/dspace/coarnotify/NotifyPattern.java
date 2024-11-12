/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.coarnotify;

/**
 * A collection of configured patterns to be met when adding COAR Notify services.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyPattern {

    private String pattern;
    private boolean multipleRequest;

    public NotifyPattern() {

    }

    public NotifyPattern(String pattern, boolean multipleRequest) {
        this.pattern = pattern;
        this.multipleRequest = multipleRequest;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean isMultipleRequest() {
        return multipleRequest;
    }

    public void setMultipleRequest(boolean multipleRequest) {
        this.multipleRequest = multipleRequest;
    }
}
