/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

public class DiscoverDateFacetField {

    private String field;
    private int start;
    private int end;
    private String gap;

    public DiscoverDateFacetField(String field, int start, int end, String gap){
        this.field = field;
        this.start = start;
        this.end = end;
        this.gap = gap;
    }


    public String getField() {
        return field;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public String getGap() {
        return gap;
    }

}