/*
 * Navigation.java
 *
 * Version: $Revision: 4461 $
 *
 * Date: $Date: 2009-10-20 02:42:11 +0000 (Tue, 20 Oct 2009) $
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
package org.dspace.app.xmlui.aspect.versioning;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;

import org.dspace.versioning.VersioningService;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import org.dspace.workflow.*;


/**
 *
 * Navigation for Versioing of Items.
 *
 * @author
 */
public class VersioningNavigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private static final Message T_context_head 				= message("xmlui.administrative.Navigation.context_head");

    private static final Message T_context_create_version= message("xmlui.aspect.versioning.VersioningNavigation.context_create_version");
    private static final Message T_context_show_version_history= message("xmlui.aspect.versioning.VersioningNavigation.context_show_version_history");




    /** Cached validity object */
	private SourceValidity validity;

	/** exports available for download */
	java.util.List<String> availableExports = null;

	 /**
     * Generate the unique cache key.
     *
     * @return The generated key hashes the src
     */
    public Serializable getKey()
    {
        Request request = ObjectModelHelper.getRequest(objectModel);

        // Special case, don't cache anything if the user is logging
        // in. The problem occures because of timming, this cache key
        // is generated before we know whether the operation has
        // succeded or failed. So we don't know whether to cache this
        // under the user's specific cache or under the anonymous user.

        if (request.getParameter("login_email")    != null ||
             request.getParameter("login_password") != null ||
               request.getParameter("login_realm")    != null )
        {
            return "0";
        }

        String key;
        if (context.getCurrentUser() != null)
        {
        	key = context.getCurrentUser().getEmail();
        	if(availableExports!=null && availableExports.size()>0){
        		for(String fileName:availableExports){
        			key+= ":"+fileName;
        		}
        	}
        }
        else
        	key = "anonymous";

        return HashUtil.hash(key);
    }

    /**
     * Generate the validity object.
     *
     * @return The generated validity object or <code>null</code> if the
     *         component is currently not cacheable.
     */
    public SourceValidity getValidity()
    {
    	if (this.validity == null)
    	{
    		// Only use the DSpaceValidity object is someone is logged in.
    		if (context.getCurrentUser() != null)
    		{
		        try {
		            DSpaceValidity validity = new DSpaceValidity();

		            validity.add(eperson);

		            Group[] groups = Group.allMemberGroups(context, eperson);
		            for (Group group : groups)
		            {
		            	validity.add(group);
		            }

		            this.validity = validity.complete();
		        }
		        catch (SQLException sqle)
		        {
		            // Just ignore it and return invalid.
		        }
    		}
    		else
    		{
    			this.validity = NOPValidity.SHARED_INSTANCE;
    		}
    	}
    	return this.validity;
    }

    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException{
    	/* Create skeleton menu structure to ensure consistent order between aspects,
    	 * even if they are never used
    	 */
        options.addList("browse");
        options.addList("account");

        List context = options.addList("context");


        // Context Administrative options  for Versioning
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);


        if(dso==null){
            // case: internal-item http://localhost:8100/internal-item?itemID=3085
            // case: admin         http://localhost:8100/admin/item?itemID=3340
            // retrieve the object from the DB
            dso=getItemById();
        }

    	if (dso instanceof Item){
    		Item item = (Item) dso;
            context.setHead(T_context_head);
            if(!isItemADataFile(item) ){
                if(isCurrentEpersonItemOwner(item) || canCurrentEPersonEditTheItem(item) || isCurrentEpersonACurator()){

                    if(isLatest(item) && item.isArchived()){
                        context.addItem().addXref(contextPath+"/item/version?itemID="+item.getID(), T_context_create_version);
                    }

                    if(hasVersionHistory(item))
                        context.addItem().addXref(contextPath+"/item/versionhistory?itemID="+item.getID(), T_context_show_version_history);
                }
            }
    	}
    }


    private DSpaceObject getItemById() throws SQLException {
        DSpaceObject dso = null;
        Request request = ObjectModelHelper.getRequest(objectModel);
        if (request.getQueryString()!=null && request.getQueryString().contains("itemID")){
            String itemId = request.getParameter("itemID");
            if (itemId != null){
                try {
                    dso = Item.find(this.context, Integer.parseInt(itemId));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return dso;
    }

    /**
     * recycle
     */
    public void recycle()
    {
        this.validity = null;
        super.recycle();
    }


    private boolean isCurrentEpersonItemOwner(Item item) throws SQLException {
        return eperson != null && item.getSubmitter() != null && item.getSubmitter().getID() == eperson.getID();
    }

    private boolean canCurrentEPersonEditTheItem(Item item) throws SQLException {
        return item.canEdit();
    }

    private boolean isCurrentEpersonACurator() throws SQLException {
        Collection dataSetColl = (Collection) HandleManager.resolveToObject(context, ConfigurationManager.getProperty("submit.publications.collection"));
        Workflow workflow = null;
        try {
            workflow = WorkflowFactory.getWorkflow(dataSetColl);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (WorkflowConfigurationException e) {
            throw new RuntimeException(e);
        }
        Role curatorRole = workflow.getRoles().get("curator");
        Group curators = WorkflowUtils.getRoleGroup(context, dataSetColl.getID(), curatorRole);
        if(curators != null && curators.isMember(context.getCurrentUser())){
            return true;
        }
        return false;
    }




    private boolean isItemADataFile(Item item){
        DCValue[] values = item.getMetadata("dc.relation.ispartof");
        if(values==null || values.length==0)
            return false;
        return true;
    }

    private boolean isLatest(Item item){
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        org.dspace.versioning.VersionHistory history = versioningService.findVersionHistory(context, item.getID());
        return (history==null || history.getLatestVersion().getItem().getID() == item.getID());
    }


    private boolean hasVersionHistory(Item item){
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        org.dspace.versioning.VersionHistory history = versioningService.findVersionHistory(context, item.getID());
        return (history!=null);
    }

}
