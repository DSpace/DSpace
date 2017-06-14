/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

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
import org.dspace.authorize.AuthorizeServiceImpl;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
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
    protected static final Message T_submit = 
    	message("xmlui.Submission.CollectionViewer.link1");
    
	
    /** Cached validity object */
    private SourceValidity validity;

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

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
            {
                return "0";
            }
                
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
     * hierarchy, when this changes they will not be reflected in the cache.
     */
    public SourceValidity getValidity()
    {
    	if (this.validity == null)
    	{
	        try
	        {
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
	
	            if (dso == null)
                {
                    return null;
                }
	
	            if (!(dso instanceof Collection))
                {
                    return null;
                }
	
	            Collection collection = (Collection) dso;
	
	            DSpaceValidity validity = new DSpaceValidity();
	            
	            // Add the actual collection;
	            validity.add(context, collection);
	            
	            // Add the eperson viewing the collection
	            validity.add(context, eperson);
	            
	            // Include any groups they are a member of
	            Set<Group> groups = groupService.allMemberGroupsSet(context, eperson);
	            for (Group group : groups)
	            {
	            	validity.add(context, group);
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
        {
            return;
        }
 
        // Set up the major variables
        Collection collection = (Collection) dso;
        
        // Only add the submit link if the user has the ability to add items.
        if (authorizeService.authorizeActionBoolean(context, collection, Constants.ADD))
        {
	        Division home = body.addDivision("collection-home","primary repository collection");
	        Division viewer = home.addDivision("collection-view","secondary");
	        String submitURL = contextPath + "/handle/" + collection.getHandle() + "/submit";
	        viewer.addPara().addXref(submitURL,T_submit); 
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
