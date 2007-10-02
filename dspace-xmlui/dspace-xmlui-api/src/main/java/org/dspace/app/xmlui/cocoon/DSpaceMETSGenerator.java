/*
 * DSpaceMETSGenerator.java
 *
 * Version: $Revision: 1.14 $
 *
 * Date: $Date: 2006/05/02 05:30:55 $
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
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.xml.sax.SAXException;

/**
 * Generate a METS document for the identified item, community or collection. The object to be rendered should be
 * identified by pasing in one of the two parameters: handle or internal. If an internal ID is given then it must
 * be of the form "type:id" i.g. item:255 or community:4 or repository:123456789. In the case of a repository the
 * id must be the handle prefix.
 * 
 * In addition to rendering a METS document there are several options which can be specified for how the mets
 * document should be rendered. All parameters are a comma seperated list of values, here is a list:
 * 
 * 
 * sections:
 * 
 * A comma seperated list of METS sections to included. The possible values are: "metsHdr", "dmdSec", 
 * "amdSec", "fileSec", "structMap", "structLink", "behaviorSec", and "extraSec". If no list is provided then *ALL*
 * sections are rendered.
 * 
 * 
 * dmdTypes:
 * 
 * A comma seperated list of metadata formats to provide as descriptive metadata. The list of avaialable metadata
 * types is defined in the dspace.cfg, disseminationcrosswalks. If no formats are provided them DIM - DSpace 
 * Intermediate Format - is used.
 * 
 * 
 * amdTypes:
 * 
 * A comma seperated list of metadata formats to provide administative metadata. DSpace does not currently
 * support this type of metadata.
 * 
 * 
 * fileGrpTypes:
 * 
 * A comma seperated list of file groups to render. For DSpace a bundle is translated into a METS fileGrp, so
 * possible values are "THUMBNAIL","CONTENT", "METADATA", etc... If no list is provided then all groups are
 * rendered.
 * 
 * 
 * structTypes:
 * 
 * A comma seperated list of structure types to render. For DSpace there is only one structType: LOGICAL. If this
 * is provided then the logical structType will be rendered, otherwise none will. The default operation is to
 * render all structure types.
 * 
 * @author scott phillips
 */
public class DSpaceMETSGenerator extends AbstractGenerator
{
	/**
	 * Generate the METS Document.
	 */
	public void generate() throws IOException, SAXException,
			ProcessingException {
		try {
			// Open a new context.
			Context context = ContextUtil.obtainContext(objectModel);
			
			// Determine which adapter to use
			AbstractAdapter adapter = resolveAdapter(context);
            if (adapter == null)
            	throw new ResourceNotFoundException("Unable to locate object.");
            
            // Configure the adapter for this request.
            configureAdapter(adapter);
            
			// Generate the METS document
			contentHandler.startDocument();
			adapter.renderMETS(contentHandler,lexicalHandler);
			contentHandler.endDocument();
			
		} catch (WingException we) {
			throw new ProcessingException(we);
		} catch (CrosswalkException ce) {
			throw new ProcessingException(ce);
		} catch (SQLException sqle) {
			throw new ProcessingException(sqle);
		}
	}
   
	
	
	/**
	 * Determine which type of adatper to use for this object, either a community, collection, item, or
	 * repository adatpter. The decisios is based upon the two supplied identifiers: a handle or an
	 * internal id. If the handle is supplied then this is resolved and the approprate adapter is
	 * picked. Otherwise the internal identifier is used to resolve the correct type of adapter.
	 * 
	 * The internal identifier must be of the form "type:id" i.g. item:255 or collection:99. In the
	 * case of a repository the handle prefix must be used.
	 * 
	 * @return Return the correct adaptor or null if none found.
	 */
	private AbstractAdapter resolveAdapter(Context context) throws SQLException 
	{			
		Request request = ObjectModelHelper.getRequest(objectModel);
        String contextPath = request.getContextPath();

        // Determine the correct adatper to use for this item
        String handle = parameters.getParameter("handle",null);
        String internal = parameters.getParameter("internal",null);
		
        AbstractAdapter adapter = null;
		 if (handle != null)
         {
			// Specified using a regular handle. 
         	DSpaceObject dso = HandleManager.resolveToObject(context, handle);
         	
         	// Handles can be either items or containers.
         	if (dso instanceof Item)
         		adapter = new ItemAdapter((Item) dso, contextPath);
         	else if (dso instanceof Collection || dso instanceof Community)
         		adapter = new ContainerAdapter(dso, contextPath);
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
         			if (item != null)
         				adapter = new ItemAdapter(item,contextPath);
         		}
         		else if ("collection".equals(type))
         		{
         			Collection collection = Collection.find(context,id);
         			if (collection != null)
         				adapter = new ContainerAdapter(collection,contextPath);
         		}
         		else if ("community".equals(type))
         		{
         			Community community = Community.find(context,id);
         			if (community != null)
         				adapter = new ContainerAdapter(community,contextPath);
         		}
         		else if ("repository".equals(type))
     			{
         			if (ConfigurationManager.getProperty("handle.prefix").equals(id))
         			adapter = new RepositoryAdapter(context,contextPath);
     			}
         		
         	}
         }
		 return adapter;
	}
	
	/**
	 * Configure the adapter according to the supplied parameters.
	 */
	public void configureAdapter(AbstractAdapter adapter)
	{
		 // Configure the adapter based upon the passed paramaters
		Request request = ObjectModelHelper.getRequest(objectModel);
        String sections = request.getParameter("sections");
        String dmdTypes = request.getParameter("dmdTypes");
        String amdTypes = request.getParameter("amdTypes");
        String fileGrpTypes = request.getParameter("fileGrpTypes");
        String structTypes = request.getParameter("structTypes");
        
        adapter.setSections(sections);
        adapter.setDmdTypes(dmdTypes);
        adapter.setAmdTypes(amdTypes);
        adapter.setFileGrpTypes(fileGrpTypes);
        adapter.setStructTypes(structTypes);
	}

}
