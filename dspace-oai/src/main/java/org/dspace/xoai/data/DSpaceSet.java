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
 * based on class by Lyncode Development Team <dspace@lyncode.com>
 * modified for LINDAT/CLARIN
 */
public class DSpaceSet extends Set {
	private static final String DefaultName = "undefined";

	public static String checkName(String name) {
		if (name != null && !name.trim().equals(""))
			return name;
		return DefaultName;
	}

	public static Set newDSpaceCommunitySet(String handle, String name) {

		return new Set("hdl_" + handle.replace('/', '_'), checkName(name));
		//return new Set("com_" + handle.replace('/', '_'), checkName(name));
	}

	public static Set newDSpaceCollectionSet(String handle, String name) {
		return new Set("hdl_" + handle.replace('/', '_'), checkName(name));
		//return new Set("col_" + handle.replace('/', '_'), checkName(name));
	}

	public DSpaceSet(Community c) {
		super("hdl_" + c.getHandle().replace('/', '_'), checkName(c.getName()));
		//super("com_" + c.getHandle().replace('/', '_'), checkName(c.getName()));
	}

	public DSpaceSet(Collection c) {
		super("hdl_" + c.getHandle().replace('/', '_'), checkName(c.getName()));
		//super("col_" + c.getHandle().replace('/', '_'), checkName(c.getName()));
	}
}
