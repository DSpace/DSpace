/**
 * $Id: DatasetGenerator.java 4440 2009-10-10 19:03:27Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/dspace/tags/dspace-1.6.2/dspace-stats/src/main/java/org/dspace/statistics/content/DatasetGenerator.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics.content;


/**
 * Represents a single facet for filtering.
 * Can be one of the axes in a table.
 * 
 * @author kevinvandevelde at atmire.com
 * Date: 23-dec-2008
 * Time: 9:39:37
 * 
 */
public abstract class DatasetGenerator {
    
    /** The type of generator can either be CATEGORY or SERIE **/
    public int DatasetType;

    public boolean includeTotal = false;

    public int getDatasetType(){
        return DatasetType;
    }

    public void setDatasetType(int datasetType){
        DatasetType = datasetType;
    }
    
    public boolean isIncludeTotal() {
        return includeTotal;
    }

    public void setIncludeTotal(boolean includeTotal) {
        this.includeTotal = includeTotal;
    }
}
