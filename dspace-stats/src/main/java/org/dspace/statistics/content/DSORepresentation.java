/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics.content;

import org.dspace.core.Constants;

/**
 * Describes the displayed representation of the statistics on a DSpaceObject
 * and its children.
 * @author TODO
 */
public class DSORepresentation {
    /** The type of DSpaceObject to be shown. */
    private Integer type;
    /** The maximum number of children to show. **/
    private Integer max;
    /** Determines if should show the DSOs as separate entities or use the sum of them. */
    private Boolean separate;

    private Integer nameLength;

    /** Construct a representation assumed to be of an ITEM. */
    public DSORepresentation() {
        setType(Constants.ITEM);
    }

    /** Construct a representation as described.
     * 
     * @param type Object type, e.g. Constants.COLLECTION
     * @param max Maximum number of children to display
     * @param separate True if children's statistics are distinct; false if summed
     */
    public DSORepresentation(Integer type, Integer max, Boolean separate) {
        this.type = type;
        this.max = max;
        this.separate = separate;
    }

    public Integer getType() {
        return type;
    }

    /**
     * @param type Object type, e.g. Constants.COLLECTION
     */
    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getMax() {
        return (max == null) ? -1 : max;
    }

    /**
     * @param max Maximum number of children to display
     */
    public void setMax(Integer max) {
        this.max = max;
    }

    public Integer getNameLength() {
        return nameLength;
    }

    public void setNameLength(Integer nameLength) {
        this.nameLength = nameLength;
    }

    public Boolean getSeparate() {
        return (separate != null) && separate;
    }

    /**
     * @param separate true for distinct child statistics; false to sum them
     */
    public void setSeparate(Boolean separate) {
        this.separate = separate;
    }
}
