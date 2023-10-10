/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.coarnotify.COARNotifyPattern;

/**
 * This class is the REST representation of the {@link COARNotifyPattern} model object
 * and acts as a data sub object for the SubmissionCOARNotifyRest class.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class SubmissionCOARNotifyPatternRest {

    private String pattern;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}
