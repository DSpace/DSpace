/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.generation.AbstractGenerator;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
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

	protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
	protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

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
            {
                throw new ResourceNotFoundException("Unable to locate object.");
            }
            
            
            // Instantiate and execute the ORE plugin
            SAXOutputter out = new SAXOutputter(contentHandler);
            DisseminationCrosswalk xwalk = (DisseminationCrosswalk)CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(DisseminationCrosswalk.class,"ore");
            
            Element ore = xwalk.disseminateElement(context, item);
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
        // Determine the correct adatper to use for this item
        String handle = parameters.getParameter("handle",null);
        String internal = parameters.getParameter("internal",null);
		
		 if (handle != null)
         {
			// Specified using a regular handle. 
         	DSpaceObject dso = handleService.resolveToObject(context, handle);
         	
         	// Handles can be either items or containers.
         	if (dso instanceof Item)
             {
                 return (Item) dso;
             }
         	else
             {
                 throw new CrosswalkException("ORE dissemination only available for DSpace Items.");
             }
         }
         else if (internal != null)
         {
        	// Internal identifier, format: "type:id".
         	String[] parts = internal.split(":");
         	
         	if (parts.length == 2)
         	{
         		String type = parts[0];
         		UUID id = UUID.fromString(parts[1]);
         		
         		if ("item".equals(type))
         		{
                     return itemService.find(context,id);
         		}
         		else
                 {
                     throw new CrosswalkException("ORE dissemination only available for DSpace Items.");
                 }
         		
         	}
         }
		 return null;
	}
	
}
