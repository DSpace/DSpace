/*
 * DSpaceOREGenerator.java
 *
 * Version: $Revision: 1.0 $
 *
 * Date: $Date: 2008/06/30 05:30:55 $
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

package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletResponse;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.generation.AbstractGenerator;
import org.dspace.app.xmlui.objectmanager.AbstractAdapter;
import org.dspace.app.xmlui.objectmanager.ContainerAdapter;
import org.dspace.app.xmlui.objectmanager.ItemAdapter;
import org.dspace.app.xmlui.objectmanager.RepositoryAdapter;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.PluginManager;
import org.dspace.handle.HandleManager;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.SAXOutputter;
import org.xml.sax.SAXException;

/**
 * Generate an ORE aggregation of a DSpace Item. The object to be rendered should be an item identified by pasing 
 * in one of the two parameters: handle or internal. The fragment parameter determines the encoding format for
 * the aggregation; only Atom is supported at this time.

 * @author Alexey Maslov
 */
public class DSpaceOREGenerator extends AbstractGenerator
{
	/**
	 * Generate the ORE Aggregation.
	 */
	public void generate() throws IOException, SAXException,
			ProcessingException {
		try {
			// Grab the context.
			Context context = ContextUtil.obtainContext(objectModel);
			
			Item item = getItem(context);
            if (item == null)
            	throw new ResourceNotFoundException("Unable to locate object.");
            
            
            // Instantiate and execute the ORE plugin
            SAXOutputter out = new SAXOutputter(contentHandler);
            DisseminationCrosswalk xwalk = (DisseminationCrosswalk)PluginManager.getNamedPlugin(DisseminationCrosswalk.class,"ore");
            
            Element ore = xwalk.disseminateElement(item);
            out.output(ore);
            
			/* Generate the METS document
			contentHandler.startDocument();
			adapter.renderMETS(contentHandler,lexicalHandler);
			contentHandler.endDocument();*/
			
		} catch (JDOMException je) {
			throw new ProcessingException(je);
		} catch (AuthorizeException ae) {
			throw new ProcessingException(ae);
		} catch (CrosswalkException ce) {
			throw new ProcessingException(ce);
		} catch (SQLException sqle) {
			throw new ProcessingException(sqle);
		}
	}
   
	
	private Item getItem(Context context) throws SQLException, CrosswalkException 
	{			
		Request request = ObjectModelHelper.getRequest(objectModel);
        String contextPath = request.getContextPath();

        // Determine the correct adatper to use for this item
        String handle = parameters.getParameter("handle",null);
        String internal = parameters.getParameter("internal",null);
		
		 if (handle != null)
         {
			// Specified using a regular handle. 
         	DSpaceObject dso = HandleManager.resolveToObject(context, handle);
         	
         	// Handles can be either items or containers.
         	if (dso instanceof Item)
         		return (Item)dso;
         	else
         		throw new CrosswalkException("ORE dissemination only available for DSpace Items.");
         }
         else if (internal != null)
         {
        	// Internal identifier, format: "type:id".
         	String[] parts = internal.split(":");
         	
         	if (parts.length == 2)
         	{
         		String type = parts[0];
         		int id = Integer.valueOf(parts[1]);
         		
         		if ("item".equals(type))
         		{
         			Item item = Item.find(context,id);
         			return item;
         		}
         		else
             		throw new CrosswalkException("ORE dissemination only available for DSpace Items.");
         		
         	}
         }
		 return null;
	}
	
}
