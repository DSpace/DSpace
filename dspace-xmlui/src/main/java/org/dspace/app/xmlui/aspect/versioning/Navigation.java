/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.versioning;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.utils.DSpace;
import org.dspace.versioning.VersioningService;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;


/**
 *
 * Navigation for Versioning of Items.
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class Navigation extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private static final Message T_context_head = message("xmlui.administrative.Navigation.context_head");
    private static final Message T_context_create_version= message("xmlui.aspect.versioning.VersioningNavigation.context_create_version");
    private static final Message T_context_show_version_history= message("xmlui.aspect.versioning.VersioningNavigation.context_show_version_history");

    /** Cached validity object */
	private SourceValidity validity;

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

                    DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
                    if(dso != null)
                    {
                        validity.add(dso);
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
            UIException, SQLException, IOException, AuthorizeException {
    	/* Create skeleton menu structure to ensure consistent order between aspects,
    	 * even if they are never used
    	 */
        options.addList("browse");
        options.addList("account");

        List context = options.addList("context");


        // Context Administrative options  for Versioning
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);


        if(dso==null)
        {
            // case: internal-item http://localhost:8100/internal-item?itemID=3085
            // case: admin         http://localhost:8100/admin/item?itemID=3340
            // retrieve the object from the DB
            dso = getItemById();
        }

    	if (dso != null && dso.getType() == Constants.ITEM)
        {
    		Item item = (Item) dso;
            if(AuthorizeManager.isAdmin(this.context, item.getOwningCollection()))
            {
                boolean headAdded=false;
                if(isLatest(item) && item.isArchived())
                {
                    context.setHead(T_context_head);
                    headAdded=true;
                    context.addItem().addXref(contextPath+"/item/version?itemID="+item.getID(), T_context_create_version);
                }

                if(hasVersionHistory(item))
                {
                    if(!headAdded)
                    {
                        context.setHead(T_context_head);
                    }
                    context.addItem().addXref(contextPath+"/item/versionhistory?itemID="+item.getID(), T_context_show_version_history);
                }
            }
    	}
    }


    private Item getItemById() throws SQLException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Item item = null;
        int itemId = Util.getIntParameter(request, "itemID");
        if (itemId != -1)
        {
            item = Item.find(this.context, itemId);
        }
        return item;
    }

    /**
     * recycle
     */
    public void recycle()
    {
        this.validity = null;
        super.recycle();
    }

    private boolean isLatest(Item item)
    {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        org.dspace.versioning.VersionHistory history = versioningService.findVersionHistory(context, item.getID());
        return (history==null || history.getLatestVersion().getItem().getID() == item.getID());
    }


    private boolean hasVersionHistory(Item item)
    {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        org.dspace.versioning.VersionHistory history = versioningService.findVersionHistory(context, item.getID());
        return (history!=null);
    }

}
