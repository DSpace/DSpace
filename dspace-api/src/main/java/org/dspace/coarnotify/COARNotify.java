/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.coarnotify;

import java.util.List;

/**
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class COARNotify {

    private String id;
    private List<String> patterns;

    public COARNotify() {

    }

    public COARNotify(String id, List<String> patterns) {
        super();
        this.id = id;
        this.patterns = patterns;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the list of COAR Notify Patterns
     *
     * @return the list of COAR Notify Patterns
     */
    public List<String> getPatterns() {
        return patterns;
    }

    /**
     * Sets the list of COAR Notify Patterns
     * @param patterns
     */
    public void setPatterns(final List<String> patterns) {
        this.patterns = patterns;
    }
}
