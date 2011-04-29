/**
 * $Id: ObjectCount.java 4405 2009-10-07 08:35:32Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/dspace/tags/dspace-1.6.2/dspace-stats/src/main/java/org/dspace/statistics/ObjectCount.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics;

/**
 * ObjectCount data structure for returning results froms tatistics searches.
 * 
 * @author mdiggory at atmire.com
 * @author ben at atmire.com
 * @author kevinvandevelde at atmire.com
 */
public class ObjectCount {
    private long count;
    private String value;

    public ObjectCount(){
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
