/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.bean;

public class RightMenuBean
{
    private String type;
    private String mode;
    private Boolean current;
    public String getType()
    {
        return type;
    }
    public void setType(String type)
    {
        this.type = type;
    }
    public String getMode()
    {
        return mode;
    }
    public void setMode(String mode)
    {
        this.mode = mode;
    }
    public void setCurrent(Boolean current)
    {
        this.current = current;
    }
    public Boolean getCurrent()
    {
        return current;
    }
    
}
