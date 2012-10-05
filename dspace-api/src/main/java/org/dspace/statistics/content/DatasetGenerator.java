/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
    protected int datasetType;

    protected boolean includeTotal = false;

    public int getDatasetType(){
        return datasetType;
    }

    public void setDatasetType(int datasetType){
        this.datasetType = datasetType;
    }
    
    public boolean isIncludeTotal() {
        return includeTotal;
    }

    public void setIncludeTotal(boolean includeTotal) {
        this.includeTotal = includeTotal;
    }
}
