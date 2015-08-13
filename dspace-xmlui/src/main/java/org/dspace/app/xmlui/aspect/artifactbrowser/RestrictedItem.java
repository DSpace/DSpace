/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.http.HttpServletResponse;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.environment.http.HttpEnvironment;
/**
 * Display an item restricted message.
 *
 * based on class by:
 * Scott Phillips
 * Mark Diggory  mdiggory at atmire dot com
 * Bolognisi fabio at atmire dot com
 * modified for LINDAT/CLARIN
 */
public class RestrictedItem extends AbstractDSpaceTransformer //implements CacheableProcessingComponent
{
    private static final Logger log = Logger.getLogger(RestrictedItem.class);

    /**
     * language strings
     */
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
    
    // replaced & withdrawn
    private static final Message T_head_item_claimed =
            message("xmlui.ArtifactBrowser.RestrictedItem.head_item_claimed");
    private static final Message T_para_item_claimed =
            message("xmlui.ArtifactBrowser.RestrictedItem.para_item_claimed");
    private static final Message T_para2_item_claimed =
            message("xmlui.ArtifactBrowser.RestrictedItem.para2_item_claimed");


    // withdrawn
    private static final Message T_head_item_withdrawn =
            message("xmlui.ArtifactBrowser.RestrictedItem.head_item_withdrawn");

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


    // Item states
    private static final Message T_para_item_restricted_auth =
            message("xmlui.ArtifactBrowser.RestrictedItem.para_item_restricted_auth");
    private static final Message T_para_item_restricted =
            message("xmlui.ArtifactBrowser.RestrictedItem.para_item_restricted");
    private static final Message T_para_item_withdrawn =
            message("xmlui.ArtifactBrowser.RestrictedItem.para_item_withdrawn");


    private static final Message T_para_login =
            message("xmlui.ArtifactBrowser.RestrictedItem.login");

    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException 
    {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        if (dso != null) {
            HandleUtil.buildHandleTrail(dso, pageMeta, contextPath);
        }
        pageMeta.addTrail().addContent(T_trail);

    }


    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException, 
            ResourceNotFoundException 
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        Division unauthorizedResource = body.addDivision("unauthorized-resource", "alert alert-error");
        List unauthorized = unauthorizedResource.addList("error-message", List.TYPE_FORM);
        boolean isWithdrawn = false;
        boolean isReplaced = false;
        boolean addFaWarning = true;
        
        if (dso == null) 
        {
            unauthorized.setHead(T_head_resource);
            unauthorized.addItem(T_para_resource);
        } 
        else if (dso instanceof Community) 
        {
            Community community = (Community) dso;
            unauthorized.setHead(T_head_community);
            unauthorized.addItem(T_para_community.parameterize(community.getMetadata("name")));
        } 
        else if (dso instanceof Collection) 
        {
            Collection collection = (Collection) dso;
            unauthorized.setHead(T_head_collection);
            unauthorized.addItem(T_para_collection.parameterize(collection.getMetadata("name")));
        } 
        else if (dso instanceof Item) 
        {
            // The dso may be an item but it could still be an item's bitstream. So let's check for the parameter.
            if (request.getParameter("bitstreamId") != null) {
            	handle_bitstream(request, unauthorized);
            } else {
            	handle_item(((Item) dso), unauthorized);
            	isWithdrawn = ((Item) dso).isWithdrawn(); 
            	isReplaced = ((Item)dso).isReplacedBy();
            	if(isWithdrawn && isReplaced){
            		unauthorizedResource.setRend("alert alert-warning");
            	}
            	addFaWarning = !(isWithdrawn && isReplaced);
            }
        } // end if Item 
        else 
        {
            // This case should not occur, but if it does just fall back to the resource message.
            unauthorized.setHead(T_head_resource);
            unauthorized.addItem(T_para_resource);
        }

        // add a login link if !loggedIn & not withdrawn
        if (!isWithdrawn && context.getCurrentUser() == null) 
        {
            unauthorized.addItem().addXref(contextPath+"/login", T_para_login);

            // Interrupt request if the user is not authenticated, so they may come back to
            // the restricted resource afterwards.
            String header = parameters.getParameter("header", null);
            String message = parameters.getParameter("message", null);
            String characters = parameters.getParameter("characters", null);

            // Interrupt this request
            AuthenticationUtil.interruptRequest(objectModel, header, message, characters);
        }
        
        if(addFaWarning){
        	unauthorized.addItem(null, "fa fa-warning fa-5x hangright").addContent(" ");
        }
        
        //Finally, set proper response. Return "404 Not Found" for all withdrawn items 
        //and "401 Unauthorized" for all restricted items
        HttpServletResponse response = (HttpServletResponse)objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);   
        if(isWithdrawn && isReplaced){
        	response.setStatus(HttpServletResponse.SC_OK);
        }
        else if (isWithdrawn)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
    
    private void handle_bitstream(Request request, List unauthorized) throws WingException 
    {
                String identifier = "unknown";
                try {
                    Bitstream bit = Bitstream.find(context, new Integer(request.getParameter("bitstreamId")));
                    if (bit != null) {
                        identifier = bit.getName();
                    }
                } catch (Exception e) {
                    // just forget it - and display the restricted message.
                    log.trace("Caught exception", e);
                }
                unauthorized.setHead(T_head_bitstream);
        		unauthorized.addItem(T_para_bitstream.parameterize(identifier));    	
    }


    private void handle_item(Item item, List unauthorized) throws WingException 
    {
                String identifier = "unknown";
                String handle = item.getHandle();
                if (handle == null || "".equals(handle)) {
            		identifier = "internal ID: " + item.getID();
                } else {
                    identifier = "hdl:" + handle;
                }

                // check why the item is restricted.
                Message title = T_head_item;
                Message status = T_para_item_restricted;
                //if item is withdrawn, display withdrawn status info
        if (item.isWithdrawn()) 
                {
            if ( item.isReplacedBy() ) {
                String itemTitle = item.getMetadata("dc.title");
                unauthorized.setHead(itemTitle);
                Metadatum[] contributorAuthor = item.getMetadataByMetadataString("dc.contributor.author");
                Metadatum[] contributorOther = item.getMetadataByMetadataString("dc.contributor.other");
                Metadatum[] mds = ArrayUtils.addAll(contributorAuthor, contributorOther);
                StringBuilder authors = new StringBuilder();
                if(mds != null && mds.length > 0){
                    String separator = "";
                    for(Metadatum md : mds){
                        authors.append(separator).append(md.value);
                        separator = "; ";
                    }
                }
                if(authors.length() > 0) {
                    unauthorized.addItem(authors.toString());
                }
                unauthorized.addItem(T_head_item_claimed);
            	unauthorized.addItem(T_para_item_claimed);
                for(String replaced : item.getReplacedBy()){
                    unauthorized.addItem().addXref(replaced, replaced);
                }
            	unauthorized.addItem(T_para2_item_claimed);
            	return;
            }else{
                unauthorized.setHead(T_head_item_withdrawn);            	
            	unauthorized.addItem(T_para_item_withdrawn.parameterize(identifier));
                unauthorized.addItem("item_status", T_para_item_withdrawn.getKey()).addContent(status);
                return;
        }
            // 

        }// if user is not authenticated, display info to authenticate
        else if (context.getCurrentUser() == null) 
        {
            status = T_para_item_restricted_auth;
        }
        unauthorized.setHead(title);
        unauthorized.addItem(T_para_item.parameterize(identifier));
        unauthorized.addItem("item_status", status.getKey()).addContent(status);
        
    }
}
