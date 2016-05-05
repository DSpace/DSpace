/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.data;

import org.dspace.content.Collection;
import org.dspace.content.Community;

import com.lyncode.xoai.dataprovider.core.Set;
import org.dspace.core.ConfigurationManager;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DSpaceSet extends Set
{
	private static final String DefaultName = "undefined";

    private static final String pfxCom = ConfigurationManager.getProperty("xoai", "set.community.prefix");
    private static final String pfxCol = ConfigurationManager.getProperty("xoai", "set.collection.prefix");

	public static String checkName (String name) {
		if (name == null || name.trim().equals(""))
			return DefaultName;
		else return name;
	}
	
    public static Set newDSpaceCommunitySet(String handle, String name)
    {
    	
        return new Set(pfxCom + handle.replace('/', '_'), checkName(name));
    }

    public static Set newDSpaceCollectionSet(String handle, String name)
    {
        return new Set(pfxCol + handle.replace('/', '_'), checkName(name));
    }

    public DSpaceSet(Community c)
    {
        super(pfxCom + c.getHandle().replace('/', '_'), checkName(c.getName()));
    }

    public DSpaceSet(Collection c)
    {
        super(pfxCol + c.getHandle().replace('/', '_'), checkName(c.getName()));
    }
}