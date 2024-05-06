/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;


/**
 * representation of the Notify Service Inbound Pattern
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyServiceInboundPatternRest {

    /**
     * https://notify.coar-repositories.org/patterns/
     */
    private String pattern;

    /**
     * the id of a bean implementing the ItemFilter
     */
    private String constraint;

    /**
     * means that the pattern is triggered automatically
     * by dspace if the item respect the filter
     */
    private boolean automatic;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getConstraint() {
        return constraint;
    }

    public void setConstraint(String constraint) {
        this.constraint = constraint;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public void setAutomatic(boolean automatic) {
        this.automatic = automatic;
    }
}
