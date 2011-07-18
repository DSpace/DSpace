/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

/**
 * Display an item restricted message.
 * 
 * @author Scott Phillips
 */
public class RestrictedItem extends AbstractDSpaceTransformer //implements CacheableProcessingComponent
{
    private static final Logger log = Logger.getLogger(RestrictedItem.class);
    
    /** language strings */
    private static final Message T_title =
        message("xmlui.ArtifactBrowser.RestrictedItem.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail = 
        message("xmlui.ArtifactBrowser.RestrictedItem.trail");

    private static final Message T_head_resource = 
        message("xmlui.ArtifactBrowser.RestrictedItem.head_resource");
    
    private static final Message T_head_community = 
        message("xmlui.ArtifactBrowser.RestrictedItem.head_community");
    
    private static final Message T_head_collection = 
        message("xmlui.ArtifactBrowser.RestrictedItem.head_collection");
    
    private static final Message T_head_item = 
        message("xmlui.ArtifactBrowser.RestrictedItem.head_item");
    
    private static final Message T_head_bitstream = 
        message("xmlui.ArtifactBrowser.RestrictedItem.head_bitstream");
    
    private static final Message T_para_resource = 
        message("xmlui.ArtifactBrowser.RestrictedItem.para_resource");
    
    private static final Message T_para_community = 
        message("xmlui.ArtifactBrowser.RestrictedItem.para_community");
    
    private static final Message T_para_collection = 
        message("xmlui.ArtifactBrowser.RestrictedItem.para_collection");
    
    private static final Message T_para_item = 
        message("xmlui.ArtifactBrowser.RestrictedItem.para_item");
    
    private static final Message T_para_bitstream = 
        message("xmlui.ArtifactBrowser.RestrictedItem.para_bitstream");
    
    
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
    	DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
    	
    	pageMeta.addMetadata("title").addContent(T_title);
               
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        if (dso != null)
        {
            HandleUtil.buildHandleTrail(dso, pageMeta, contextPath);
        }
        pageMeta.addTrail().addContent(T_trail);
        
    }

  
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {   
    	Request  request = ObjectModelHelper.getRequest(objectModel);
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        
        Division unauthorized = body.addDivision("unauthorized-resource","primary");

        if (dso == null)
        {
            unauthorized.setHead(T_head_resource);
            unauthorized.addPara(T_para_resource);
        } 
        else if (dso instanceof Community)
        {
        	Community community = (Community) dso;
        	unauthorized.setHead(T_head_community);
            unauthorized.addPara(T_para_community.parameterize(community.getMetadata("name")));
        } 
        else if (dso instanceof Collection)
        {
        	Collection collection = (Collection) dso;
        	unauthorized.setHead(T_head_collection);
            unauthorized.addPara(T_para_collection.parameterize(collection.getMetadata("name")));
        }
        else if (dso instanceof Item)
        {
        	 // The dso may be an item but it could still be an item's bitstream. So let's check for the parameter.
        	if (request.getParameter("bitstreamId") != null)
        	{
        		String identifier = "unknown";
        		try {
        			Bitstream bit = Bitstream.find(context, Integer.valueOf(request.getParameter("bitstreamId")));
	        		if (bit != null) {
	        			identifier = bit.getName();
	        		}
        		}
        		catch(Exception e) {
        			// just forget it - and display the restricted message.
                    log.trace("Caught exception", e);
        		}
        		unauthorized.setHead(T_head_bitstream);
                unauthorized.addPara(T_para_bitstream.parameterize(identifier));
        	} 
        	else
        	{
        		String identifier = "unknown";
        		String handle = dso.getHandle();
            	if (handle == null || "".equals(handle))
            	{
            		identifier =  "internal ID: " + dso.getID();
            	}
            	else
            	{
            		identifier = "hdl:"+handle;
            	}
        		unauthorized.setHead(T_head_item);
                unauthorized.addPara(T_para_item.parameterize(identifier));
        	}
        } 
        else
        {
        	// This case should not occure, but if it does just fall back to the resource message.
        	unauthorized.setHead(T_head_resource);
            unauthorized.addPara(T_para_resource);
        }
      
    }
}
