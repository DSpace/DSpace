/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package edu.tamu.metadatatreebrowser;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.discovery.SimpleSearch;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchServiceException;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

/**
 *  Custom search for the Metadata Tree Browser  
 *
 * @author Scott Phillips, http://www.scottphillips.com/
 * @author Alexey Maslov
 * @author Jason Savell <jsavell@library.tamu.edu>
 */
public class BrowseNode extends SimpleSearch {
	private static final Logger log = Logger.getLogger(BrowseNode.class);

	private MetadataTreeNode node;
   
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    
    private static final Message T_dspace_home =
            message("xmlui.general.dspace_home");
    
    
    /**
     * Add Page metadata.
     */
    public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException {
       	pageMeta.addMetadata("title").addContent(node.getName());
       	
        // Add the trail back to the repository root.
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if ((dso instanceof org.dspace.content.Collection) || (dso instanceof Community)) {
            HandleUtil.buildHandleTrail(context, dso, pageMeta, contextPath, true);
        }
    }

    /**
     * build the DRI page representing the body of the search query. This
     * provides a widget to generate a new query and list of search results if
     * present.
     */
    public void addBody(Body body) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException {
        DSpaceObject currentScope = getScope();
		if (node == null) {
			getNode();
			if (node == null) {
				return;
			}
		}
		String baseURL = contextPath + "/handle/" + currentScope.getHandle()+ "/mdbrowse";
		
		// Display the Parent bread crumb
		Division div = body.addDivision("metadata-tree-browser-node","primary");

		// Nested parent list
		Division parentDiv = div.addDivision("parent-div");
		org.dspace.app.xmlui.wing.element.List parentList = parentDiv.addList("parent-list");
		parentList.addItemXref(contextPath + "/handle/" + currentScope.getHandle(), currentScope instanceof org.dspace.content.Collection ? "Collection Home" : "Community Home");

		if (!(node.getParent() == null || node.getParent().isRoot())) {
			parentList = parentList.addList("parent-sub-list");
			for(MetadataTreeNode parent : node.getParents()) {
				if (!parent.isRoot()) {
					String nodeURL = baseURL + "?node=" + parent.getId();
					parentList.addItemXref(nodeURL, parent.getName());
					parentList = parentList.addList("parent-sub-list");
				}
			}
		}
		
		Division contentDiv = div.addDivision("node-content-div","primary");
		contentDiv.setHead(node.getName());
		
		// Display any children
		if (node.hasChildren()) {
			Division childDiv = contentDiv.addDivision("child-div");
			org.dspace.app.xmlui.wing.element.List childList = childDiv.addList("child-list");
			for(MetadataTreeNode child : node.getChildren()) {
				Bitstream thumbnail = bitstreamService.find(context, child.getThumbnailId());
				String thumbnailURL = contextPath + "/bitstream/id/"+thumbnail.getID()+"/?sequence="+thumbnail.getSequenceID();
				String nodeURL = baseURL + "?node=" + child.getId();

				org.dspace.app.xmlui.wing.element.Item item = childList.addItem();
				item.addFigure(thumbnailURL, nodeURL, "node-thumbnail");
				item.addXref(nodeURL, child.getName(),"node-label");
			}
		}
		
        // Add the result division
		// Display any items
		if (node.hasContent()) {
			try {
	            buildSearchResultsDivision(contentDiv);
	        } catch (SearchServiceException e) {
	            throw new UIException(e.getMessage(), e);
	        }
		}
    }
    
    @Override
    public DiscoverQuery prepareQuery(DSpaceObject scope, String query, String[] fqs) throws UIException, SearchServiceException {
    	
    	super.prepareQuery(scope, query, fqs);
    	getNode();

    	
        String fieldLabel = null;
		try {
			fieldLabel = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("xmlui.mdbrowser."+getScope().getHandle()+".field");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        if (fieldLabel == null || fieldLabel.length() == 0) {
 			fieldLabel = "dc.relation.ispartof";
        }
        queryArgs.addFilterQueries(fieldLabel+": \""+node.getFieldValue()+"\"");
        return queryArgs;
 }
    
    @Override
    protected String generateURL(Map<String, String> parameters)
            throws UIException {
		
	   Request request = ObjectModelHelper.getRequest(objectModel);
	   parameters.put("node",request.getParameter("node"));

	   if (parameters.get("page") == null) {
		   parameters.put("page", request.getParameter("page"));
	   }
	   return AbstractDSpaceTransformer.generateURL("mdbrowse", parameters);

    }
    
    /**
     * Get the browse node from the URL parameter, if none is found the empty
     * string is returned.
     */
    protected String getNodeId() throws UIException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String query = decodeFromURL(request.getParameter("node"));
        if (query == null)
        {
            return "";
        }
        return query.trim();
    }
    
    private MetadataTreeNode getNode() {
        try {
 		    String nodeString = getNodeId();
 		    
 			DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
 			
 		    MetadataTreeNode root = MetadataTreeService.getInstance().getFullTree(context, dso); 
 			
 			node = root.findById(Integer.valueOf(nodeString));
 			
        } catch (UIException e) {
            log.error(e.getMessage(), e);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return node;
    }

}
