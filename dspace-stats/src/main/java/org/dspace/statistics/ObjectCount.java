/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

/**
 * Data structure for returning results from statistics searches.
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
