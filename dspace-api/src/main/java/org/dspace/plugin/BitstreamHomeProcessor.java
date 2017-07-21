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
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Bitstream home processor
 * 
 * @author Pascarelli Luigi Andrea
 *
 */
public interface BitstreamHomeProcessor
{
	/**
	 * execute the process
	 * 
	 * @param context       the DSpace context
	 * @param request	    the HTTP request
	 * @param response	    the HTTP response
	 * @param bitstream
	 * 
	 * @throws PluginException	any particular problem with the plugin execution
	 * @throws AuthorizeException	Authorisation errors during plugin execution
	 */
	void process(Context context, HttpServletRequest request,
    		HttpServletResponse response, Bitstream item)
		throws PluginException, AuthorizeException;
	
}