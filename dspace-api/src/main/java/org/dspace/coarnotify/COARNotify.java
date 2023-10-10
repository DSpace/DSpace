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

    private String name;
    private String id;
    private List<COARNotifyPattern> coarNotifyPatterns;

    public COARNotify() {
        super();
    }

    public COARNotify(String id, String name, List<COARNotifyPattern> coarNotifyPatterns) {
        super();
        this.id = id;
        this.name = name;
        this.coarNotifyPatterns = coarNotifyPatterns;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
    public List<COARNotifyPattern> getCoarNotifyPatterns() {
        return coarNotifyPatterns;
    }

    /**
     * Sets the list of COAR Notify Patterns
     * @param coarNotifyPatterns
     */
    public void setCoarNotifyPatterns(final List<COARNotifyPattern> coarNotifyPatterns) {
        this.coarNotifyPatterns = coarNotifyPatterns;
    }
}
