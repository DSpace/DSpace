/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree
 */
package org.dspace.resourcesync;

import org.dspace.core.Context;
import org.openarchives.resourcesync.ChangeListArchive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
/**
 * @author Richard Jones
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Andrea Petrucci (andrea.petrucci at 4science.it)
 *
 */
public class DSpaceChangeListArchive
{
    private Context context;
    private Map<String, Date> changeLists  = new HashMap<String, Date>();
    private ChangeListArchive cla = null;
    private UrlManager um;

    public UrlManager getUm() {
		return um;
	}

	public void setUm(UrlManager um) {
		this.um = um;
	}

	public DSpaceChangeListArchive(Context context)
    {

        this.context = context;
        this.um = new UrlManager();
    }

    public void addChangeList(String loc, Date date)
    {
        this.changeLists.put(loc, date);
    }

    public void readInSource(InputStream in)
    {
        this.cla = new ChangeListArchive(in);
    }

    public void serialise(OutputStream out)
            throws IOException
    {
        if (this.cla == null)
        {
            this.cla = new ChangeListArchive(this.um.capabilityList());
        }

        for (String loc : this.changeLists.keySet())
        {
            this.cla.addChangeList(loc, this.changeLists.get(loc));
        }

        this.cla.serialise(out);
    }
}
