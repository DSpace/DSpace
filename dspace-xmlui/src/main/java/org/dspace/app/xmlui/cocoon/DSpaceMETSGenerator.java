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
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.xml.sax.SAXException;

/**
 * Generate a METS document for the identified item, community or collection. The object to be rendered should be
 * identified by passing in one of the two parameters: handle or internal. If an internal ID is given then it must
 * be of the form "type:id" i.g. item:255 or community:4 or repository:123456789. In the case of a repository the
 * id must be the handle prefix.
 * 
 * In addition to rendering a METS document there are several options which can be specified for how the mets
 * document should be rendered. All parameters are a comma-separated list of values, here is a list:
 * 
 * 
 * sections:
 * 
 * A comma-separated list of METS sections to included. The possible values are: "metsHdr", "dmdSec", 
 * "amdSec", "fileSec", "structMap", "structLink", "behaviorSec", and "extraSec". If no list is provided then *ALL*
 * sections are rendered.
 * 
 * 
 * dmdTypes:
 * 
 * A comma-separated list of metadata formats to provide as descriptive metadata. The list of available metadata
 * types is defined in the dspace.cfg, dissemination crosswalks. If no formats are provided them DIM - DSpace 
 * Intermediate Format - is used.
 * 
 * 
 * amdTypes:
 * 
 * A comma-separated list of metadata formats to provide administrative metadata. DSpace does not currently
 * support this type of metadata.
 * 
 * 
 * fileGrpTypes:
 * 
 * A comma-separated list of file groups to render. For DSpace a bundle is translated into a METS fileGrp, so
 * possible values are "THUMBNAIL","CONTENT", "METADATA", etc... If no list is provided then all groups are
 * rendered.
 * 
 * 
 * structTypes:
 * 
 * A comma-separated list of structure types to render. For DSpace there is only one structType: LOGICAL. If this
 * is provided then the logical structType will be rendered, otherwise none will. The default operation is to
 * render all structure types.
 * 
 * @author Scott Phillips
 */
public class DSpaceMETSGenerator extends AbstractGenerator
{
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
   	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

	/**
	 * Generate the METS Document.
     * @throws java.io.IOException passed through.
     * @throws org.xml.sax.SAXException passed through.
     * @throws org.apache.cocoon.ProcessingException on error.
	 */
    @Override
	public void generate() throws IOException, SAXException, ProcessingException {
		try {
			// Open a new context.
			Context context = ContextUtil.obtainContext(objectModel);
			
			// Determine which adapter to use
			AbstractAdapter adapter = resolveAdapter(context);
            if (adapter == null)
            {
                throw new ResourceNotFoundException("Unable to locate object.");
            }
            
            // Configure the adapter for this request.
            configureAdapter(adapter);
            
			// Generate the METS document
			contentHandler.startDocument();
			adapter.renderMETS(context, contentHandler,lexicalHandler);
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
	 * Determine which type of adapter to use for this object, either a community, collection, item, or
	 * repository adapter. The decision is based upon the two supplied identifiers: a handle or an
	 * internal id. If the handle is supplied then this is resolved and the appropriate adapter is
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

        // Determine the correct adapter to use for this item
        String handle = parameters.getParameter("handle",null);
        String internal = parameters.getParameter("internal",null);
		
        AbstractAdapter adapter = null;
		 if (handle != null)
         {
            // Specified using a regular handle.
            DSpaceObject dso = handleService.resolveToObject(context, handle);

            // Handles can be either items or containers.
            if (dso instanceof Item)
            {
                adapter = new ItemAdapter(context, (Item) dso, contextPath);
            }
         	else if (dso instanceof Collection || dso instanceof Community)
            {
                adapter = new ContainerAdapter(context, dso, contextPath);
            }
         }
         else if (internal != null)
         {
        	// Internal identifier, format: "type:id".
         	String[] parts = internal.split(":");
         	
         	if (parts.length == 2)
         	{
         		String type = parts[0];
                       String strid = parts[1];
         		UUID id = null;

                        // Handle prefixes must be treated as strings
                        // all non-repository types need integer IDs
                        if ("repository".equals(type))
                        {
                                if (handleService.getPrefix().equals(strid))
                                {
                                    adapter = new RepositoryAdapter(context, contextPath);
                                }
                        }
                        else
                        {
                               id = UUID.fromString(parts[1]);
         			if ("item".equals(type))
         			{
         				Item item = itemService.find(context,id);
         				if (item != null)
                         {
                             adapter = new ItemAdapter(context, item, contextPath);
                         }
         			}
         			else if ("collection".equals(type))
         			{
         				Collection collection = collectionService.find(context,id);
         				if (collection != null)
                         {
                             adapter = new ContainerAdapter(context, collection, contextPath);
                         }
         			}
         			else if ("community".equals(type))
         			{
         				Community community = communityService.find(context,id);
         				if (community != null)
                         {
                             adapter = new ContainerAdapter(context, community, contextPath);
                         }
         			}
			}
         	}
         }
		 return adapter;
	}
	
	/**
	 * Configure the adapter according to the supplied parameters.
     * @param adapter the adapter.
	 */
	public void configureAdapter(AbstractAdapter adapter)
	{
        // Configure the adapter based upon the passed parameters
        Request request = ObjectModelHelper.getRequest(objectModel);
        String sections = request.getParameter("sections");
        String dmdTypes = request.getParameter("dmdTypes");
        String techMDTypes = request.getParameter("techMDTypes");
        String rightsMDTypes = request.getParameter("rightsMDTypes");
        String sourceMDTypes = request.getParameter("sourceMDTypes");
        String digiprovMDTypes = request.getParameter("digiprovMDTypes");
        String fileGrpTypes = request.getParameter("fileGrpTypes");
        String structTypes = request.getParameter("structTypes");
        
        adapter.setSections(sections);
        adapter.setDmdTypes(dmdTypes);
        adapter.setTechMDTypes(techMDTypes);
        adapter.setRightsMDTypes(rightsMDTypes);
        adapter.setSourceMDTypes(sourceMDTypes);
        adapter.setDigiProvMDTypes(digiprovMDTypes);
        adapter.setFileGrpTypes(fileGrpTypes);
        adapter.setStructTypes(structTypes);
	}

}
