/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.plugin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface that must be implemented by any plugin wanting to be called at the
 * inception of the Item page (in HandleServlet). Classes that
 * implement the process method and appear in the configuration will be run
 * before the at the start of preparing the item home page has any chance
 * to continue its execution. <b>Note that the plugin is executed also before 
 * than the READ permission on the item is checked</b>
 * 
 * @author Andrea Bollini
 * 
 */
public interface ItemHomeProcessor
{
	/**
	 * execute the process
	 * 
	 * @param context       the DSpace context
	 * @param request	    the HTTP request
	 * @param response	    the HTTP response
	 * @param item         	the item object whose home page we are on
	 * 
	 * @throws PluginException	any particular problem with the plugin execution
	 * @throws AuthorizeException	Authorisation errors during plugin execution
	 */
	void process(Context context, HttpServletRequest request,
    		HttpServletResponse response, Item item)
		throws PluginException, AuthorizeException;
	
}