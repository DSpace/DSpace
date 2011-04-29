/**
 * $Id: DatasetTypeGenerator.java 4440 2009-10-10 19:03:27Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/dspace/tags/dspace-1.6.2/dspace-stats/src/main/java/org/dspace/statistics/content/DatasetTypeGenerator.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics.content;


/**
 * Represents a simple string facet for filtering.
 * Doesn't offer any special interaction.
 *
 * @author kevinvandevelde at atmire.com
 * Date: 23-dec-2008
 * Time: 12:44:27
 * 
 */
public class DatasetTypeGenerator extends DatasetGenerator {

    /** The type of our generator (EXAMPLE: country) **/
    private String type;
    /** The number of values shown (max) **/
    private int max;


    public DatasetTypeGenerator() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }
}
