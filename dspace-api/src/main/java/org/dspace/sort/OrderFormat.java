/*
 * OrderFormat.java
 *
 * Version: $Revision: 1.0 $
 *
 * Date: $Date: 2007/03/02 11:22:13 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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

package org.dspace.sort;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.core.PluginManager;
import org.dspace.sort.OrderFormatDelegate;
import org.dspace.sort.OrderFormatAuthor;
import org.dspace.sort.OrderFormatTitle;
import org.dspace.sort.OrderFormatText;

/**
 * Class implementing static helpers for anywhere that interacts with the sort columns
 * (ie. ItemsByAuthor.sort_author, ItemsByTitle.sort_title)
 *
 * This class maps index 'types' to delegates that implement the sort string creation
 * 
 * Types can be defined or configured using the plugin manager:
 * 
 * plugin.named.org.dspace.sort.OrderFormatDelegate=
 * 		org.dspace.sort.OrderFormatTitleMarc21=title
 * 		org.dspace.sort.OrderFormatAuthor=author
 * 
 * The following standard types have been defined by default, but can be reconfigured
 * via the plugin manager:
 * 
 * author	= org.dspace.sort.OrderFormatAuthor
 * title	= org.dspace.sort.OrderFormatTitle
 * text 	= org.dspace.sort.OrderFormatText
 * 
 * IMPORTANT - If you change any of the orderings, you need to rebuild the browse sort columns
 * (ie. run 'index-all', or 'dsrun org.dspace.browse.InitializeBrowse')
 * 
 * @author Graham Triggs
 * @version $Revision: 1.0 $
 */
public class OrderFormat
{
	private final static Logger log = LogManager.getLogger(OrderFormat.class);

	public final static String AUTHOR = "author";
	public final static String TITLE  = "title";
	public final static String TEXT   = "text";
	
	// Array of all available order delegates - avoids excessive calls to plugin manager
	private final static String[] delegates = PluginManager.getAllPluginNames(OrderFormatDelegate.class);

    private final static OrderFormatDelegate authorDelegate = new OrderFormatAuthor();
    private final static OrderFormatDelegate titleDelegate  = new OrderFormatTitle();
    private final static OrderFormatDelegate textDelegate   = new OrderFormatText();
    
    /**
     * Generate a sort string for the given DC metadata
     */
    public static String makeSortString(String value, String language, String type)
    {
    	OrderFormatDelegate delegate = null;
    	
        // If there is no value, return null
        if (value == null)
            return null;

    	// If a named index has been supplied
    	if (type != null && type.length() > 0)
    	{
    		// Use a delegate if one is configured
        	if ((delegate = OrderFormat.getDelegate(type)) != null)
        	{
        		return delegate.makeSortString(value, language);
        	}

            // No delegates found, so apply defaults
            if (type.equalsIgnoreCase(OrderFormat.AUTHOR) && authorDelegate != null)
            {
              return authorDelegate.makeSortString(value, language);
            }

            if (type.equalsIgnoreCase(OrderFormat.TITLE) && titleDelegate != null)
            {
              return titleDelegate.makeSortString(value, language);
            }

            if (type.equalsIgnoreCase(OrderFormat.TEXT) && textDelegate != null)
            {
              return textDelegate.makeSortString(value, language);
            }
    	}

    	return value;
    }

    /**
     * Retrieve the named delegate
     */
    private static OrderFormatDelegate getDelegate(String name)
    {
   		if (name != null && name.length() > 0)
   		{
   			// Check the cached array of names to see if the delegate has been configured
	   		for (int idx = 0; idx < delegates.length; idx++)
	   		{
	   			if (delegates[idx].equals(name))
	   			{
	   				return (OrderFormatDelegate)PluginManager.getNamedPlugin(OrderFormatDelegate.class, name);
	   			}
	   		}
   		}
   		
    	return null;
    }
}
