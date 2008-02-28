/*
 * CollectionViewer.java
 *
 * Version: $Revision: 1.21 $
 *
 * Date: $Date: 2006/07/27 18:24:34 $
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
package org.dspace.app.xmlui.aspect.submission;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.xml.sax.SAXException;

/**
 * Add a single link to the display item page that allows
 * the user to submit a new item to this collection.
 * 
 * @author Scott Phillips
 */
public class CollectionViewer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
	
	/** Language Strings */
    protected static final Message T_title = 
        message("xmlui.Submission.SelectCollection.title");
	
    /** Cached validity object */
    private SourceValidity validity;
    
    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey()
    {
        try
        {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            if (dso == null)
                return "0";
                
            return HashUtil.hash(dso.getHandle());
        }
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not
            // cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     * 
     * The validity object will include the collection being viewed and 
     * all recently submitted items. This does not include the community / collection
     * hierarch, when this changes they will not be reflected in the cache.
     */
    public SourceValidity getValidity()
    {
    	if (this.validity == null)
    	{
	        try
	        {
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
	
	            if (dso == null)
	                return null;
	
	            if (!(dso instanceof Collection))
	                return null;
	
	            Collection collection = (Collection) dso;
	
	            DSpaceValidity validity = new DSpaceValidity();
	            
	            // Add the actual collection;
	            validity.add(collection);
	            
	            // Add the eperson viewing the collection
	            validity.add(eperson);
	            
	            // Include any groups they are a member of
	            Group[] groups = Group.allMemberGroups(context, eperson);
	            for (Group group : groups)
	            {
	            	validity.add(group);
	            }
	            
	            this.validity = validity.complete();
	        }
	        catch (Exception e)
	        {
	            // Just ignore all errors and return an invalid cache.
	        }
    	}
    	return this.validity;
    }

    /**
     * Add a single link to the view item page that allows the user 
     * to submit to the collection.
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Collection))
            return;
 
        // Set up the major variables
        Collection collection = (Collection) dso;
        
        // Only add the submit link if the user has the ability to add items.
        if (AuthorizeManager.authorizeActionBoolean(context, collection, Constants.ADD))
        {
	        Division home = body.addDivision("collection-home","primary repository collection");
	        Division viewer = home.addDivision("collection-view","secondary");
	        String submitURL = contextPath + "/handle/" + collection.getHandle() + "/submit";
	        viewer.addPara().addXref(submitURL,"Submit a new item to this collection"); 
        }
        
    }
    
    /**
     * Recycle
     */
    public void recycle() 
    {   
        this.validity = null;
        super.recycle();
    }
}
