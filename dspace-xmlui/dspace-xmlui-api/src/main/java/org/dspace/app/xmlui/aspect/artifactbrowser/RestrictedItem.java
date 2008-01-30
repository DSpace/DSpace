/*
 * RestrictedItem.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2006/08/08 20:58:55 $
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
package org.dspace.app.xmlui.aspect.artifactbrowser;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.utils.URIUtil;
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
import org.dspace.uri.IdentifierFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Display an item restricted message.
 * 
 * @author Scott Phillips
 */
public class RestrictedItem extends AbstractDSpaceTransformer //implements CacheableProcessingComponent
{
    /** language strings */
    private static final Message T_title =
        message("xmlui.ArtifactBrowser.RestrictedItem.title");
    
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    private static final Message T_trail = 
        message("xmlui.ArtifactBrowser.RestrictedItem.trail");

    private static final Message T_head = 
        message("xmlui.ArtifactBrowser.RestrictedItem.head");
    
    private static final Message T_para = 
        message("xmlui.ArtifactBrowser.RestrictedItem.para");
    
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
    	DSpaceObject dso = URIUtil.resolve(objectModel);
    	
    	pageMeta.addMetadata("title").addContent(T_title);
               
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        if (dso != null)
        	HandleUtil.buildHandleTrail(dso,pageMeta,contextPath);
        pageMeta.addTrail().addContent(T_trail);
        
    }

  
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {   
    	Request  request = ObjectModelHelper.getRequest(objectModel);
        DSpaceObject dso = URIUtil.resolve(objectModel);
        
        String type = null;
        String identifier = null;
        if (dso == null)
        {
        	type = "resource";
        	identifier = "unknown";
        } 
        else if (dso instanceof Community)
        {
        	Community community = (Community) dso;
        	identifier = community.getMetadata("name");
        	type = "community";
        } 
        else if (dso instanceof Collection)
        {
        	Collection collection = (Collection) dso;
        	identifier = collection.getMetadata("name");
        	type = "collection";
        } 
        else 
        {
        	String handle = IdentifierFactory.getCanonicalForm(dso);
        	type = "item";
        	if (handle == null || "".equals(handle))
        	{
        		identifier =  "internal ID: " + dso.getID();
        	}
        	else
        	{
        		identifier = handle;
        	}
        	
        	if (request.getParameter("bitstreamId")!=null){
        		type = "bitstream";
        		try{
        			Bitstream bit = Bitstream.find(context, new Integer(request.getParameter("bitstreamId")));
	        		if(bit!=null){
	        			identifier = bit.getName();
	        		}
	        		else{
	        			identifier = "unknown";
	        		}
        		}
        		catch(Exception e){
        			
        		}
        	}
        }
        	
        Division unauthorized = body.addDivision("unauthorized-item","primary");
        unauthorized.setHead(T_head.parameterize(type));
        
        unauthorized.addPara(T_para.parameterize(new Object[]{type,identifier}));
    }
}
