/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

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
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.Reference;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.ItemCountException;
import org.dspace.browse.ItemCounter;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

/**
 * Display a single community. This includes a full text search, browse by list,
 * community display and a list of recent submissions.
 *     private static final Logger log = Logger.getLogger(DSpaceFeedGenerator.class);

 * @author Scott Phillips
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class CommunityViewer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    

    public static final Message T_untitled = 
    	message("xmlui.general.untitled");

    private static final Message T_head_sub_communities =
        message("xmlui.ArtifactBrowser.CommunityViewer.head_sub_communities");
    
    private static final Message T_head_sub_collections =
        message("xmlui.ArtifactBrowser.CommunityViewer.head_sub_collections");
    

    /** Cached validity object */
    private SourceValidity validity;

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();


    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            
            if (dso == null)
            {
                return "0";  // no item, something is wrong
            }
            
            return HashUtil.hash(dso.getHandle());
        } 
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     * 
     * This validity object includes the community being viewed, all 
     * sub-communites (one level deep), all sub-collections, and 
     * recently submitted items.
     */
    public SourceValidity getValidity() 
    {
        if (this.validity == null)
    	{
            Community community = null;
	        try {
	            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
	            
	            if (dso == null)
                {
                    return null;
                }
	            
	            if (!(dso instanceof Community))
                {
                    return null;
                }
	            
	            community = (Community) dso;
	            
	            DSpaceValidity validity = new DSpaceValidity();
	            validity.add(context, community);
	            
	            List<Community> subCommunities = community.getSubcommunities();
	            List<Collection> collections = community.getCollections();
	            // Sub communities
	            for (Community subCommunity : subCommunities)
	            {
	                validity.add(context, subCommunity);
	                
	                // Include the item count in the validity, only if the value is shown.
	                boolean showCount = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("webui.strengths.show");
	                if (showCount)
	        		{
	                    try {	
	                    	int size = new ItemCounter(context).getCount(subCommunity);
	                    	validity.add("size:"+size);
	                    } catch(ItemCountException e) { /* ignore */ }
	        		}
	            }
	            // Sub collections
	            for (Collection collection : collections)
	            {
	                validity.add(context, collection);
	                
	                // Include the item count in the validity, only if the value is shown.
	                boolean showCount = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("webui.strengths.show");
	                if (showCount)
	        		{
	                    try {
	                    	int size = new ItemCounter(context).getCount(collection);
	                    	validity.add("size:"+size);
	                    } catch(ItemCountException e) { /* ignore */ }
	        		}
	            }

	            this.validity = validity.complete();
	        } 
	        catch (Exception e)
	        {
	            // Ignore all errors and invalidate the cache.
	        }

    	}
        return this.validity;
    }
    
    
    /**
     * Add the community's title and trail links to the page's metadata
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Community))
        {
            return;
        }

        // Set up the major variables
        Community community = (Community) dso;
        // Set the page title
        String name = community.getName();
        if (name == null || name.length() == 0)
        {
            pageMeta.addMetadata("title").addContent(T_untitled);
        }
        else
        {
            pageMeta.addMetadata("title").addContent(name);
        }

        // Add the trail back to the repository root.
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        HandleUtil.buildHandleTrail(context, community, pageMeta,contextPath);
        
        // Add RSS links if available
        String[] formats = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("webui.feed.formats");
		if ( formats != null )
		{
			for (String format : formats)
			{
				// Remove the protocol number, i.e. just list 'rss' or' atom'
				String[] parts = format.split("_");
				if (parts.length < 1)
                {
                    continue;
                }
				
				String feedFormat = parts[0].trim()+"+xml";
					
				String feedURL = contextPath+"/feed/"+format.trim()+"/"+community.getHandle();
				pageMeta.addMetadata("feed", feedFormat).addContent(feedURL);
			}
		}
    }

    /**
     * Display a single community (and reference any sub communites or
     * collections)
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Community))
        {
            return;
        }

        // Set up the major variables
        Community community = (Community) dso;
        List<Community> subCommunities = community.getSubcommunities();
        List<Collection> collections = community.getCollections();

        // Build the community viewer division.
        Division home = body.addDivision("community-home", "primary repository community");
        String name = community.getName();
        if (name == null || name.length() == 0)
        {
            home.setHead(T_untitled);
        }
        else
        {
            home.setHead(name);
        }

        // The search / browse box placeholder, this division will be populated either in the browse or discovery aspect
        {
            home.addDivision("community-search-browse",
                    "secondary search-browse");
        }

        // Add main reference:
        {
        	Division viewer = home.addDivision("community-view","secondary");
        	
            ReferenceSet referenceSet = viewer.addReferenceSet("community-view",
                    ReferenceSet.TYPE_DETAIL_VIEW);
            Reference communityInclude = referenceSet.addReference(community);

            // If the community has any children communities also reference them.
            if (subCommunities != null && subCommunities.size() > 0)
            {
                ReferenceSet communityReferenceSet = communityInclude
                        .addReferenceSet(ReferenceSet.TYPE_SUMMARY_LIST,null,"hierarchy");

                communityReferenceSet.setHead(T_head_sub_communities);

                // Subcommunities
                for (Community subCommunity : subCommunities)
                {
                    communityReferenceSet.addReference(subCommunity);
                }
            }
            if (collections != null && collections.size() > 0)
            {
                ReferenceSet communityReferenceSet = communityInclude
                        .addReferenceSet(ReferenceSet.TYPE_SUMMARY_LIST,null,"hierarchy");

                communityReferenceSet.setHead(T_head_sub_collections);
                       

                // Subcollections
                for (Collection collection : collections)
                {
                    communityReferenceSet.addReference(collection);
                }

            }
        } // main reference
    }
    


    /**
     * Recycle
     */
    public void recycle()
    {
        // Clear out our item's cache.
        this.validity = null;
        super.recycle();
    }


}
