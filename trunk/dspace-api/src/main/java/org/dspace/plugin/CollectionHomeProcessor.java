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
import org.dspace.content.Collection;
import org.dspace.core.Context;

/**
 * Interface that must be implemented by any plugin wanting to be called at the
 * inception of the Collection home page (in HandleServlet). Classes that
 * implement the process method and appear in the configuration will be run
 * before the at the start of preparing the collection home page has any chance
 * to continue its execution
 * 
 * @author Richard Jones
 * 
 */
public interface CollectionHomeProcessor
{
	/**
	 * execute the process
	 * 
	 * @param context       the DSpace context
	 * @param request	    the HTTP request
	 * @param response	    the HTTP response
	 * @param collection	the collection object whose home page we are on
	 * 
	 * @throws PluginException	any particular problem with the plugin execution
	 * @throws AuthorizeException	Authorisation errors during plugin execution
	 */
	void process(Context context, HttpServletRequest request,
    		HttpServletResponse response, Collection collection)
		throws PluginException, AuthorizeException;
	
}