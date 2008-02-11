/*
 * DSpaceObjectManager.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2006/03/20 22:39:04 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.xmlui.objectmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.dspace.app.xmlui.wing.ObjectManager;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.browse.BrowseItem;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;


/**
 * The Wing ObjectManager implemented specificaly for DSpace. This manager 
 * is able identify all DSpace items, communities, and collections.
 * 
 * @author Scott Phillips
 */

public class DSpaceObjectManager implements ObjectManager
{
  
	/** List of all managed DSpaceObjects */
	private List<DSpaceObject> dsos = new ArrayList<DSpaceObject>();
	
    /**
     * Manage the given object, if this manager is unable to manage the object then false must be returned.
     * 
     * @param object
     *            The object to be managed.
     * @return The object identifiers
     */
    public boolean manageObject(Object object) throws WingException
    {
    	// First check that the object is of a type we can manage.
    	if (object instanceof BrowseItem)
    	{
    		dsos.add((BrowseItem) object);
    		return true;
    	}
    	else if (object instanceof Item)
    	{
    		dsos.add((Item) object);
    		return true;
    	}
    	else if (object instanceof Collection)
    	{
    		dsos.add((Collection) object);
    		return true;
    	}
    	else if (object instanceof Community)
    	{
    		dsos.add((Community) object);
    		return true;
    	}
    	
    	// We are unable to manage this object.
    	return false;
    }
	
	
    /**
     * Return the metadata URL of the supplied object, assuming 
     * it's a DSpace item, community or collection.
     * 
     */
	public String getObjectURL(Object object) throws WingException 
	{
		if (object instanceof DSpaceObject)
		{
			DSpaceObject dso = (DSpaceObject) object;
			String handle = dso.getHandle();
			
			// If the object has a handle then refrence it by it's handle.
			if (handle != null)
			{
				return "/metadata/handle/" + handle + "/mets.xml";
			}
			else
			{
				// No handle then refrence it by an internal ID.
				if (dso instanceof Item || dso instanceof BrowseItem)
		    	{
		    		return "/metadata/internal/item/" + dso.getID() + "/mets.xml";
		    	}
		    	else if (object instanceof Collection)
		    	{
		    		return "/metadata/internal/collection/" + dso.getID() + "/mets.xml";
		    	}
		    	else if (object instanceof Community)
		    	{
		    		return "/metadata/internal/community/" + dso.getID() + "/mets.xml";
		    	}
			}
		}
		
		return null;
	}
	
	/**
	 * Return a pretty specific string giving a hint to the theme as to what
	 * type of DSpace object is being refrenced.
	 */
	public String getObjectType(Object object) throws WingException 
	{
		if (object instanceof Item || object instanceof BrowseItem)
    	{
    		return "DSpace Item";
    	}
    	else if (object instanceof Collection)
    	{
    		return "DSpace Collection";
    	}
    	else if (object instanceof Community)
    	{
    		return "DSpace Community";
    	}
			
		return null;
	}
	
	/**
	 * Return a globably unique identifier for the repository. For dspace, we use the handle prefix.
	 */
	public String getRepositoryIdentifier(Object object) throws WingException
	{
		return ConfigurationManager.getProperty("handle.prefix");
	}
	
	/**
	 * Return the metadata URL for this repository.
	 */
	public String getRepositoryURL(Object object) throws WingException
	{
		String handlePrefix = ConfigurationManager.getProperty("handel.prefix");
		return "/metadata/internal/repository/"+handlePrefix +"/mets.xml";
	}
	
	/**
	 * For the DSpace implementation we just return a hash of one entry which contains
	 * a reference to this repository's metadata.
	 */
	public HashMap<String,String> getAllManagedRepositories() throws WingException
	{
		String handlePrefix = ConfigurationManager.getProperty("handle.prefix");
		
		HashMap<String,String> allRepositories = new HashMap<String,String>();
		allRepositories.put(handlePrefix, "/metadata/internal/repository/"+handlePrefix +"/mets.xml");
		
		return allRepositories;
	}
}
