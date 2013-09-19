/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.Bundle;
import org.dspace.handle.HandleManager;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.avalon.framework.parameters.Parameters;

import java.sql.SQLException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class UsageLoggerAction extends AbstractAction {

    public Map act(Redirector redirector, SourceResolver sourceResolver, Map objectModel, String string, Parameters parameters) throws Exception {
        try{
            Request request = ObjectModelHelper.getRequest(objectModel);
            Context context = ContextUtil.obtainContext(objectModel);
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if(dso == null){
                //We might have a bitstream
                dso = findBitstream(context, parameters);
            }
            logDspaceObject(request, dso, context);
        }catch(Exception e){
            // Ignore, we cannot let this crash
            // TODO: log this
            e.printStackTrace();
        }

        // Finished, allow to pass.
        return null;
    }

    public static void logDspaceObject(Request request, DSpaceObject dso, Context context){
        if(dso == null)
        {
            return;
        }

        try {
        	
			new DSpace().getEventService().fireEvent(
					new UsageEvent(
							UsageEvent.Action.VIEW,
							(HttpServletRequest)request,
							ContextUtil.obtainContext((HttpServletRequest)request),
							dso));
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }

    private Bitstream findBitstream(Context context, Parameters par) throws SQLException {
        // Get our parameters that identify the bitstream
        int itemID = par.getParameterAsInteger("itemID", -1);
        int bitstreamID = par.getParameterAsInteger("bitstreamID", -1);
        String handle = par.getParameter("handle", null);

        int sequence = par.getParameterAsInteger("sequence", -1);
        String name = par.getParameter("name", null);


        // Resolve the bitstream
        Bitstream bitstream = null;
        Item item;
        DSpaceObject dso;

        if (bitstreamID > -1)
        {
            // Direct reference to the individual bitstream ID.
            bitstream = Bitstream.find(context, bitstreamID);
        }
        else if (itemID > -1)
        {
            // Referenced by internal itemID
            item = Item.find(context, itemID);

            if (sequence > -1)
            {
                bitstream = findBitstreamBySequence(item, sequence);
            }
            else if (name != null)
            {
                bitstream = findBitstreamByName(item, name);
            }
        }
        else if (handle != null)
        {
            // Reference by an item's handle.
            dso = HandleManager.resolveToObject(context,handle);

            if (dso instanceof Item)
            {
                item = (Item)dso;

                if (sequence > -1)
                {
                    bitstream = findBitstreamBySequence(item,sequence);
                }
                else if (name != null)
                {
                    bitstream = findBitstreamByName(item,name);
                }
            }
        }
        return bitstream;
    }

    /**
     * Find the bitstream identified by a sequence number on this item.
     *
     * @param item A DSpace item
     * @param sequence The sequence of the bitstream
     * @return The bitstream or null if none found.
     */
    private Bitstream findBitstreamBySequence(Item item, int sequence) throws SQLException
    {
    	if (item == null)
        {
            return null;
        }

    	Bundle[] bundles = item.getBundles();
        for (Bundle bundle : bundles)
        {
            Bitstream[] bitstreams = bundle.getBitstreams();

            for (Bitstream bitstream : bitstreams)
            {
            	if (bitstream.getSequenceID() == sequence)
            	{
            		return bitstream;
                }
            }
        }
        return null;
    }

    /**
     * Return the bitstream from the given item that is identified by the
     * given name. If the name has prepended directories they will be removed
     * one at a time until a bitstream is found. Note that if two bitstreams
     * have the same name then the first bitstream will be returned.
     *
     * @param item A DSpace item
     * @param name The name of the bitstream
     * @return The bitstream or null if none found.
     */
    private Bitstream findBitstreamByName(Item item, String name) throws SQLException
    {
    	if (name == null || item == null)
        {
            return null;
        }

    	// Determine our the maximum number of directories that will be removed for a path.
    	int maxDepthPathSearch = 3;
    	if (ConfigurationManager.getProperty("xmlui.html.max-depth-guess") != null)
        {
            maxDepthPathSearch = ConfigurationManager.getIntProperty("xmlui.html.max-depth-guess");
        }

    	// Search for the named bitstream on this item. Each time through the loop
    	// a directory is removed from the name until either our maximum depth is
    	// reached or the bitstream is found. Note: an extra pass is added on to the
    	// loop for a last ditch effort where all directory paths will be removed.
    	for (int i = 0; i < maxDepthPathSearch+1; i++)
    	{
    	   	// Search through all the bitstreams and see
	    	// if the name can be found
	    	Bundle[] bundles = item.getBundles();
	        for (Bundle bundle : bundles)
	        {
	            Bitstream[] bitstreams = bundle.getBitstreams();

	            for (Bitstream bitstream : bitstreams)
	            {
	            	if (name.equals(bitstream.getName()))
	            	{
	            		return bitstream;
	            	}
	            }
	        }

	        // The bitstream was not found, so try removing a directory
	        // off of the name and see if we lost some path information.
	        int indexOfSlash = name.indexOf('/');

	        if (indexOfSlash < 0)
            {
                // No more directories to remove from the path, so return null for no
                // bitstream found.
                return null;
            }

	        name = name.substring(indexOfSlash+1);

	        // If this is our next to last time through the loop then
	        // trim everything and only use the trailing filename.
    		if (i == maxDepthPathSearch-1)
    		{
    			int indexOfLastSlash = name.lastIndexOf('/');
    			if (indexOfLastSlash > -1)
                {
                    name = name.substring(indexOfLastSlash + 1);
                }
    		}

    	}

    	// The named bitstream was not found and we exausted the maximum path depth that
    	// we search.
    	return null;
    }
}
