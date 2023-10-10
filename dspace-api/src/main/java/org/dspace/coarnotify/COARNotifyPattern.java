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
public class COARNotifyPattern {

    private String pattern;

    public COARNotifyPattern() {

    }

    public COARNotifyPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}


