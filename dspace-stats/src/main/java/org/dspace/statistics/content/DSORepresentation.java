/**
 * $Id: DSORepresentation.java 4440 2009-10-10 19:03:27Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/dspace/tags/dspace-1.6.2/dspace-stats/src/main/java/org/dspace/statistics/content/DSORepresentation.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics.content;

import org.dspace.core.Constants;

public class DSORepresentation {
    /** The children of our dspaceobject to be shown **/
    private Integer type;
    /** The maximum number to show **/
    private Integer max;
    /** Determines if should show the dso's as seperate entities or use the sum of them **/
    private Boolean separate;

    private Integer nameLength;


    public DSORepresentation() {
        setType(Constants.ITEM);
    }

    public DSORepresentation(Integer type, Integer max, Boolean separate) {
        this.type = type;
        this.max = max;
        this.separate = separate;
    }


    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getMax() {
        return (max == null) ? -1 : max;
    }

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

    public void setSeparate(Boolean separate) {
        this.separate = separate;
    }
}
