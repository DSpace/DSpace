/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics;

import java.util.List;

public class StatSubscriptionViewBean
{
    private String objectName;
    private Object object;
    private int type;
    private String id;
    private List<Integer> freqs;
        
    public String getObjectName()
    {
        return objectName;
    }
    public void setObjectName(String objectName)
    {
        this.objectName = objectName;
    }
    public Object getObject()
    {
        return object;
    }
    public void setObject(Object object)
    {
        this.object = object;
    }
    public int getType()
    {
        return type;
    }
    public void setType(int type)
    {
        this.type = type;
    }
    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        this.id = id;
    }
    public List<Integer> getFreqs()
    {
        return freqs;
    }
    public void setFreqs(List<Integer> freqs)
    {
        this.freqs = freqs;
    }
    
}
