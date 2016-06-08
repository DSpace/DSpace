/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.objectmanager;

import java.util.HashMap;
import java.util.Map;

import org.dspace.app.xmlui.wing.ObjectManager;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.handle.HandleServiceImpl;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;


/**
 * The Wing ObjectManager implemented specifically for DSpace. This manager 
 * is able identify all DSpace items, communities, and collections.
 * 
 * @author Scott Phillips
 */

public class DSpaceObjectManager implements ObjectManager
{

	protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

    /**
     * Manage the given object, if this manager is unable to manage the object then false must be returned.
     * 
     * @param object
     *            The object to be managed.
     * @return The object identifiers
     */
    @Override
    public boolean manageObject(Object object)
    {
    	// Check that the object is of a type we can manage.
    	return (object instanceof Item) || (object instanceof Collection)
			    || (object instanceof Community);
    }
	
	
    /**
     * Return the metadata URL of the supplied object, assuming 
     * it's a DSpace item, community or collection.
     */
    @Override
	public String getObjectURL(Object object) throws WingException 
	{
		if (object instanceof DSpaceObject)
		{
			DSpaceObject dso = (DSpaceObject) object;
			String handle = dso.getHandle();
			
			// If the object has a handle then reference it by its handle.
			if (handle != null)
			{
				return "/metadata/handle/" + handle + "/mets.xml";
			}
			else
			{
				// No handle then reference it by an internal ID.
				if (dso instanceof Item)
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
	 * type of DSpace object is being referenced.
	 */
    @Override
	public String getObjectType(Object object)
	{
		if (object instanceof Item)
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
     * Return a globally unique identifier for the repository. For dspace, we
     * use the handle prefix.
     */
    @Override
	public String getRepositoryIdentifier(Object object) throws WingException
	{
		return handleService.getPrefix();
	}
	
	/**
	 * Return the metadata URL for this repository.
     * @param object unused.
     * @return path to the metadata document.
     * @throws org.dspace.app.xmlui.wing.WingException never.
	 */
	public String getRepositoryURL(Object object) throws WingException
	{
		String handlePrefix = handleService.getPrefix();
		return "/metadata/internal/repository/"+handlePrefix +"/mets.xml";
	}
	
	/**
	 * For the DSpace implementation we just return a hash of one entry which contains
	 * a reference to this repository's metadata.
	 */
    @Override
	public Map<String,String> getAllManagedRepositories() throws WingException
	{
		String handlePrefix = handleService.getPrefix();
		
		Map<String,String> allRepositories = new HashMap<String,String>();
		allRepositories.put(handlePrefix, "/metadata/internal/repository/"+handlePrefix +"/mets.xml");
		
		return allRepositories;
	}
}
