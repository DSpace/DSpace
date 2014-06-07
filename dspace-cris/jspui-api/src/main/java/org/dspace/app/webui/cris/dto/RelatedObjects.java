/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.dto;

import java.util.ArrayList;
import java.util.List;

public class RelatedObjects
{
    private int totalRecords;
    
    private int filterRecords;

    private List<RelatedObject> objects = new ArrayList<RelatedObject>();

    public int getTotalRecords()
    {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords)
    {
        this.totalRecords = totalRecords;
    }

    public int getFilterRecords()
    {
        return filterRecords;
    }

    public void setFilterRecords(int filterRecords)
    {
        this.filterRecords = filterRecords;
    }

    public List<RelatedObject> getObjects()
    {
        return objects;
    }

    public void setObjects(List<RelatedObject> objects)
    {
        this.objects = objects;
    }
}
