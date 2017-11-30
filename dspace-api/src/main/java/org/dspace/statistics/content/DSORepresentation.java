/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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

    public final Integer getType() {
        return type;
    }

    /**
     * @param type Object type, e.g. Constants.COLLECTION
     */
    public final void setType(Integer type) {
        this.type = type;
    }

    public final Integer getMax() {
        return (max == null) ? -1 : max;
    }

    /**
     * @param max Maximum number of children to display
     */
    public final void setMax(Integer max) {
        this.max = max;
    }

    public final Integer getNameLength() {
        return nameLength;
    }

    public final void setNameLength(Integer nameLength) {
        this.nameLength = nameLength;
    }

    public final Boolean getSeparate() {
        return (separate != null) && separate;
    }

    /**
     * @param separate true for distinct child statistics; false to sum them
     */
    public final void setSeparate(Boolean separate) {
        this.separate = separate;
    }
}
