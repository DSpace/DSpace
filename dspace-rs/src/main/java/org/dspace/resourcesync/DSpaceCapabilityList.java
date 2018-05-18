/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.resourcesync;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.openarchives.resourcesync.CapabilityList;
import org.openarchives.resourcesync.ResourceSync;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Richard Jones
 *
 */
public class DSpaceCapabilityList
{
    private Context context;
    private String describedBy;
    private UrlManager um;
    private boolean resourceList;
    private boolean changeListArchive;
    private boolean resourceDump;
    private boolean changeList;
    private boolean changeDump;
    private String latestChangeList;
    private String latestChangeDump;

    public DSpaceCapabilityList(Context context, boolean resourceList, boolean changeListArchive, boolean resourceDump, boolean changeList,
    		boolean changeDump,String latestChangeList,String latestChangeDump,UrlManager um)
    {
        this.context = context;
        this.describedBy = ConfigurationManager.getProperty("resourcesync", "capabilitylist.described-by");
        if ("".equals(this.describedBy))
        {
            this.describedBy = null;
        }
        this.um = um;
        this.resourceList = resourceList;
        this.changeListArchive = changeListArchive;
        this.resourceDump = resourceDump;
        this.changeList = changeList;
        this.latestChangeList = latestChangeList;
        this.changeDump = changeDump;
        this.latestChangeDump = latestChangeDump;
    }

    public void serialise(OutputStream out)
            throws IOException
    {
        String rlUrl = this.resourceList ? this.um.resourceList() : null;
        String claUrl = this.changeListArchive ? this.um.changeListArchive() : null;
        String rdUrl = this.resourceDump ? this.um.resourceDump() : null;
        String rsdUrl = this.um.resourceSyncDescription();

        CapabilityList cl = new CapabilityList(describedBy, null);
        if (rlUrl != null)
        {
            cl.setResourceList(rlUrl);
        }
        if (claUrl != null)
        {
            cl.setChangeListArchive(claUrl);
        }
        if (rdUrl != null)
        {
            cl.setResourceDump(rdUrl);
        }
        if (rsdUrl != null)
        {
            cl.addLn(ResourceSync.REL_UP, rsdUrl);
        }

        if (this.changeList && this.latestChangeList != null)
        {
            cl.setChangeList(this.latestChangeList);
        }
        if (this.changeDump && this.latestChangeDump != null)
        {
            cl.setChangeDump(this.latestChangeDump);
        }
       
        cl.serialise(out);
    }
}
