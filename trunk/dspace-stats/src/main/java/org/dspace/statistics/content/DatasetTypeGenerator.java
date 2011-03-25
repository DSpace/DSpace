/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
