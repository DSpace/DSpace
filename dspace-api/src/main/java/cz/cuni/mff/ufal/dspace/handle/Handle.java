/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.handle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.event.Event;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.Identifier;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/* Created for LINDAT/CLARIN */
/**
 * Class representing a handle
 * 
 * @author Michal Josífko 
 */
public class Handle extends DSpaceObject implements Identifier
{
	
	public static final int HANDLE = -1;
	
    /** log4j category */
    private static Logger log = Logger.getLogger(Handle.class);

    /** Our context */
    private Context ourContext;

    /** The table row corresponding to this item */
    private TableRow handleRow;    

    /**
     * Construct a handle object from a database row.
     * 
     * @param context
     *            the context this object exists in
     * @param row
     *            the corresponding row in the table
     */
    public Handle(Context context, TableRow row) throws SQLException
    {
        ourContext = context;
        handleRow = row;

        // Cache ourselves
        context.cache(this, row.getIntColumn("handle_id"));         
       
    }
    
    /**
     * Get a handle from the database.
     * 
     * @param context
     *            DSpace context object
     * @param id
     *            ID of the handle
     * 
     * @return the handle, or null if the ID is invalid.
     */
    public static Handle find(Context context, int id) throws SQLException
    {
        // First check the cache
        Handle fromCache = (Handle) context
                .fromCache(Handle.class, id);

        if (fromCache != null)
        {
            return fromCache;
        }

        TableRow row = DatabaseManager.find(context, "handle", id);

        if (row == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_handle",
                        "not_found,handle_id=" + id));
            }

            return null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "find_handle",
                        "handle_id=" + id));
            }

            return new Handle(context, row);
        }
    }   
    
    /**
     * Find handle object by handle identifier
     *
     * @param context
     *            DSpace context object
     * @param handle
     *            handle identifier
     * @return handle object or null if not found 
     * @throws SQLException
     */
    public static Handle findByHandle(Context context, String handle)
            throws SQLException
    {    	    	        
        TableRow row = DatabaseManager.findByUnique(context, "handle",
                "handle", handle);

        if (row == null)
        {
            return null;
        }
        else
        {
            // First check the cache
        	Handle fromCache = (Handle) context.fromCache(Handle.class, row
                    .getIntColumn("handle_id"));

            if (fromCache != null)
            {
                return fromCache;
            }
            else
            {
                return new Handle(context, row);
            }
        }                
    }

    /**
     * Get a list of all handles in the system.
     * 
     * @param context
     *            DSpace context object
     * 
     * @return the handles in the system
     */
    public static List<Handle> findAll(Context context) throws SQLException
    {    	
        TableRowIterator tri = DatabaseManager.queryTable(context, "handle",
                "SELECT * FROM handle");

        List<Handle> handles = new ArrayList<Handle>();

        try
        {
            while (tri.hasNext())
            {
                TableRow row = tri.next();

                // First check the cache
                Handle fromCache = (Handle) context.fromCache(
                		Handle.class, row.getIntColumn("handle_id"));

                if (fromCache != null)
                {
                	handles.add(fromCache);
                }
                else
                {
                	handles.add(new Handle(context, row));
                }
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }               

        return handles;        
    }
    
    /**
     * Create new handle, with supplied handle identifier.
     *
     * @param context
     *            DSpace context object
     * @param dso 
     * 			  the object associated with the new handle  
     * 
     * @return the newly created handle
     */
    public static Handle create(Context context, DSpaceObject dso)
            throws SQLException, AuthorizeException
    {
    	Handle h = null;
    	
    	if(canCreate(context)) {
    		
    		if(dso == null) {
    			h = new Handle(context, DatabaseManager.create(context, "handle"));    			
    		}
    		else {
    			String handle = HandleManager.createHandle(context, dso);    			        
    			h = Handle.findByHandle(context, handle);
    		}    		    		
	        
	        log.info(LogManager.getHeader(context, "create_handle",
	                "handle_id=" + h.getID())
	                + ",handle=" + h.getHandle());
    	}
	
        return h;
    }

    /**
     * Create new handle, with supplied handle identifier.
     *
     * @param context
     *            DSpace context object
     * @param dso 
     * 			  the object associated with the new handle
     * @param handle 
     * 	          the handle
     * 
     * @return the newly created handle
     */
    public static Handle create(Context context, DSpaceObject dso, String handle)
            throws SQLException, AuthorizeException
    {    
    	Handle h = null;
    	
    	if(canCreate(context)) {
    		
    		if(dso == null) {
    			h = new Handle(context, DatabaseManager.create(context, "handle"));
    			h.setHandle(handle);
    		}
    		else {
    			String returnedHandle = HandleManager.createHandle(context, dso, handle);    			        
    			h = Handle.findByHandle(context, returnedHandle);
    		}
	        
	        log.info(LogManager.getHeader(context, "create_handle",
	                "handle_id=" + h.getID())
	                + ",handle=" + h.getHandle());	        	        
	        
    	}
	
        return h;
    }
    
    /**
     * Update the handle
     */
    public void update() throws SQLException, AuthorizeException
    {
        // Check authorization
        if (canEdit()) 
        {

	        log.info(LogManager.getHeader(ourContext, "update_handle",
	                "handle_id=" + getID()));
	
	        DatabaseManager.update(ourContext, handleRow);
        }
    }       
    
    /**
     * Delete the handle
     */
    public void delete() throws SQLException, AuthorizeException, IOException
    {
    	if(canDelete()) {
	        log.info(LogManager.getHeader(ourContext, "delete_handle",
	                "handle_id=" + getID()));
	
	        ourContext.addEvent(new Event(Event.DELETE, HANDLE, getID(), getHandle()));
	
	        // Remove from cache
	        ourContext.removeCached(this, getID());
		
	        // Delete handle row
	        DatabaseManager.delete(ourContext, handleRow);	    
    	}
    }
    
    /**
     * Get the internal ID of this handle
     * 
     * @return the internal identifier
     */
    public int getID()
    {
        return handleRow.getIntColumn("handle_id");
    }
   
    /**
     * Get the handle (persistent identifier)
     * 
     * @return the handle
     */
    public String getHandle()
    {        
    	return handleRow.getStringColumn("handle");
    }
    
    /**
     * Set the handle
     *  
     */    
    public void setHandle(String handle)
    {        
    	handleRow.setColumn("handle", handle);
    }
    
    /**
     * Get the url
     * 
     * @return the url
     *  
     */
    public String getURL()
    {        
    	return handleRow.getStringColumn("url");
    }
    
    public void setURL(String handle)
    {        
    	handleRow.setColumn("url", handle);
    }   
    
    public int getResourceTypeID()
    {        
    	return handleRow.getIntColumn("resource_type_id");
    }
    
    public void setResourceTypeID(int resourceTypeID)
    {        
    	if(resourceTypeID == -1) 
    		handleRow.setColumnNull("resource_type_id");
    	else 
    		handleRow.setColumn("resource_type_id", resourceTypeID);    	
    }
    
    public int getResourceID()
    {            	
    	return handleRow.getIntColumn("resource_id");
    }
    
    public void setResourceID(int resourceID)
    {     
    	if(resourceID == -1) 
    		handleRow.setColumnNull("resource_id");
    	else 
    		handleRow.setColumn("resource_id", resourceID);
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Handle
     * as this object, <code>false</code> otherwise
     * 
     * @param other
     *            object to compare to
     * 
     * @return <code>true</code> if object passed in represents the same
     *         handle as this object
     */
    public boolean equals(Object other)
    {
        if (!(other instanceof Handle))
        {
            return false;
        }

        return (getID() == ((Handle) other).getID());
    }        
    
    public static boolean canCreate(Context context) throws AuthorizeException, SQLException
    {        
    	
        return AuthorizeManager.isAdmin(context);
    }

    public boolean canEdit() throws AuthorizeException, SQLException
    {        
        return AuthorizeManager.isAdmin(ourContext);
    }
    
    public boolean canDelete() throws AuthorizeException, SQLException
    {        
        return AuthorizeManager.isAdmin(ourContext);
    }

	@Override
	public int getType() {		
		return HANDLE;
	}

	@Override
	public String getName() { 
		return getHandle();
	}		
	
	public boolean isInternalResource() {		
		return (getURL() == null || getURL().isEmpty());
	}

	@Override
	public void updateLastModified() {
		// TODO Auto-generated method stub
		
	}
			
}
