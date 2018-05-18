/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.resourcesync;

import org.dspace.content.Site;
import org.dspace.core.ConfigurationManager;

/**
 * @author Richard Jones
 *
 */
public class UrlManager
{
    private String base;  

    public UrlManager() {
    	
    }
    public UrlManager(String handle)
    {
        this.base = ConfigurationManager.getProperty("resourcesync", "base-url");
        if (!this.base.endsWith("/"))
        {
            this.base += "/";
        }
        if (handle != null && !handle.equals(Site.getSiteHandle()))
        {
            String dir = handle.replace("/", "-");
        	this.base += dir + "/";
        }
    }

    public String resourceSyncDescription()
    {
        return this.base + FileNames.resourceSyncDocument;
    }

    public String capabilityList()
    {
        return this.base + FileNames.capabilityList;
    }

    public String resourceList()
    {
        return this.base + FileNames.resourceList;
    }

    public String changeListArchive()
    {
        return this.base + FileNames.changeListArchive;
    }

    public String changeList(String filename)
    {
        return this.base + filename;
    }

    public String resourceDump()
    {
        return this.base + FileNames.resourceDump;
    }
    
    public String resourceDumpZip()
    {
        return this.base + FileNames.resourceDumpZip;
    }
    public String changeDump(String filename)
    {
    	return this.base + filename;
    }
}
