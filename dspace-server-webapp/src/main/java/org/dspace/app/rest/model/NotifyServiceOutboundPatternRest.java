/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 * representation of the Notify Service Outbound Pattern
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyServiceOutboundPatternRest {

    private String pattern;

    private String constraint;

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
}
