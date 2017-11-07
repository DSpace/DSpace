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

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DSpaceSet extends Set
{
    public static Set newDSpaceCommunitySet(String handle, String name)
    {
        return new Set("com_" + handle.replace('/', '_'), name);
    }

    public static Set newDSpaceCollectionSet(String handle, String name)
    {
        return new Set("col_" + handle.replace('/', '_'), name);
    }

    public DSpaceSet(Community c)
    {
        super("com_" + c.getHandle().replace('/', '_'), c.getName());
    }

    public DSpaceSet(Collection c)
    {
        super("col_" + c.getHandle().replace('/', '_'), c.getName());
    }
}
