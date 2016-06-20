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
import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.List;
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
import org.dspace.core.Constants;
import org.dspace.core.LogManager;

import org.xml.sax.SAXException;

/**
 * Display a list of Communities and collections.
 * 
 * This item may be configured so that it will only display up to a specific depth,
 * and may include or exclude collections from the tree.
 * 
 * <p>The configuration option available:
 *
 * <pre>
 * {@code <depth exclude-collections="true">999</depth>}
 * </pre>
 *
 * @author Scott Phillips
 */
public class CommunityBrowser extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    private static final Logger log = Logger.getLogger(CommunityBrowser.class);

    /** Language Strings */
    public static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
    
    public static final Message T_title =
        message("xmlui.ArtifactBrowser.CommunityBrowser.title");
    
    public static final Message T_trail =
        message("xmlui.ArtifactBrowser.CommunityBrowser.trail");
    
    public static final Message T_head =
        message("xmlui.ArtifactBrowser.CommunityBrowser.head");
    
    public static final Message T_select =
        message("xmlui.ArtifactBrowser.CommunityBrowser.select");
    
    /** Should collections be excluded from the list */
    protected boolean excludeCollections = false;

    /** The default depth if one is not provided by the sitemap */
    private static final int DEFAULT_DEPTH = 999;

    /** What depth is the maximum depth of the tree */
    protected int depth = DEFAULT_DEPTH;

    /** Cached version the community / collection hierarchy */
    protected TreeNode root;
    
    /** cached validity object */
    private SourceValidity validity;

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

    /**
     * Set the component up, pulling any configuration values from the sitemap
     * parameters.
     *
     * @param resolver source resolver.
     * @param objectModel object model.
     * @param src source.
     * @param parameters sitemap parameters.
     * @throws org.apache.cocoon.ProcessingException passed through.
     * @throws org.xml.sax.SAXException passed through.
     * @throws java.io.IOException passed through.
     */
    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters)
            throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver, objectModel, src, parameters);

        depth = parameters.getParameterAsInteger("depth", DEFAULT_DEPTH);
        excludeCollections = parameters.getParameterAsBoolean(
                "exclude-collections", false);
    }

    /**
     * Generate the unique caching key.
     * This key must be unique within the space of this component.
     * @return the key.
     */
    @Override
    public Serializable getKey()
    {
    	boolean full = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("xmlui.community-list.render.full", true);
        return HashUtil.hash(depth + "-" + excludeCollections + "-" + (full ? "true" : "false"));
    }

    /**
     * Generate the cache validity object.
     * 
     * The validity object will include a list of all communities 
     * and collections being browsed along with their logo bitstreams.
     * @return validity.
     */
    @Override
    public SourceValidity getValidity()
    {
    	if (validity == null)
    	{
	        try {
	            DSpaceValidity theValidity = new DSpaceValidity();
	            
	            TreeNode treeRoot = buildTree(communityService.findAllTop(context));
	            
	            Stack<TreeNode> stack = new Stack<>();
	            stack.push(treeRoot);
	            
	            while (!stack.empty())
	            {
	                TreeNode node = stack.pop();
	                
	                theValidity.add(context, node.getDSO());
	                
	                // If we are configured to use collection strengths (i.e. item counts) then include that number in the validity.
	                boolean showCount = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("webui.strengths.show");
	                if (showCount)
	        		{
	                    try
	                    {	//try to determine Collection size (i.e. # of items)
	                    	
	                    	int size = new ItemCounter(context).getCount(node.getDSO());
	                    	theValidity.add("size:"+size);
	                    }
	                    catch(ItemCountException e) { /* ignore */ }
	        		}
	                
	                
	                for (TreeNode child : node.getChildren())
	                {
	                    stack.push(child);
	                }
	            }
	            
	            // Check if we are configured to assume validity.
	            String assumeCacheValidity = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("xmlui.community-list.cache");
	            if (assumeCacheValidity != null)
                {
                    theValidity.setAssumedValidityDelay(assumeCacheValidity);
                }
	            
	            this.validity = theValidity.complete();
	        } 
	        catch (SQLException sqle) 
	        {
	            // ignore all errors and return an invalid cache.
	        }
            log.info(LogManager.getHeader(context, "view_community_list", ""));
    	}
    	return this.validity;
    }

    /**
     * Add a page title and trail links.
     * @throws org.xml.sax.SAXException passed through.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // Set the page title
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }

    /**
     * Add a community-browser division that includes references to community and
     * collection metadata.
     * @throws org.xml.sax.SAXException passed through.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     * @throws org.dspace.app.xmlui.utils.UIException passed through.
     * @throws java.sql.SQLException passed through.
     * @throws java.io.IOException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Division division = body.addDivision("comunity-browser", "primary");
        division.setHead(T_head);
        division.addPara(T_select);

        TreeNode treeRoot = buildTree(communityService.findAllTop(context));

        boolean full = DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("xmlui.community-list.render.full", true);

        if (full)
        {
	        ReferenceSet referenceSet = division.addReferenceSet("community-browser",
	                ReferenceSet.TYPE_SUMMARY_LIST,null,"hierarchy");
	        
	        java.util.List<TreeNode> rootNodes = treeRoot.getChildrenOfType(Constants.COMMUNITY);
	        
	        for (TreeNode node : rootNodes)
	        {
	            buildReferenceSet(referenceSet,node);   
	        }
        }
        else
        {
        	List list = division.addList("comunity-browser");
        	
        	java.util.List<TreeNode> rootNodes = treeRoot.getChildrenOfType(Constants.COMMUNITY);
 	        
 	        for (TreeNode node : rootNodes)
 	        {
 	            buildList(list,node);   
 	        }
        	
        }
    } 
    
    /**
     * Recursively build an includeset of the community / collection hierarchy based upon
     * the given NodeTree.
     * 
     * @param referenceSet The include set
     * @param node The current node of the hierarchy.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void buildReferenceSet(ReferenceSet referenceSet, TreeNode node) throws WingException
    {
        DSpaceObject dso = node.getDSO();
        
        Reference objectInclude = referenceSet.addReference(dso);
        
        // Add all the sub-collections;
        java.util.List<TreeNode> collectionNodes = node.getChildrenOfType(Constants.COLLECTION);
        if (collectionNodes != null && collectionNodes.size() > 0)
        {
            ReferenceSet collectionSet = objectInclude.addReferenceSet(ReferenceSet.TYPE_SUMMARY_LIST);
            
            for (TreeNode collectionNode : collectionNodes)
            {
                collectionSet.addReference(collectionNode.getDSO());
            }
        }
        
        // Add all the sub-communities
        java.util.List<TreeNode> communityNodes = node.getChildrenOfType(Constants.COMMUNITY);
        if (communityNodes != null && communityNodes.size() > 0)
        {
            ReferenceSet communitySet = objectInclude.addReferenceSet(ReferenceSet.TYPE_SUMMARY_LIST);
            
            for (TreeNode communityNode : communityNodes)
            {
                buildReferenceSet(communitySet,communityNode);
            }
        }
    }
    
    /**
     * Recursively build a list of the community / collection hierarchy based upon
     * the given NodeTree.
     * 
     * @param list The parent list
     * @param node The current node of the hierarchy.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void buildList(List list, TreeNode node) throws WingException
    {
        DSpaceObject dso = node.getDSO();
        
        String name = null;
        if (dso instanceof Community)
        {
            name = ((Community) dso).getName();
        }
        else if (dso instanceof Collection)
        {
            name = ((Collection) dso).getName();
        }
        
        String aURL = contextPath + "/handle/"+dso.getHandle();
        list.addItem().addHighlight("bold").addXref(aURL, name);
        
        List subList = null;
        
        // Add all the sub-collections;
        java.util.List<TreeNode> collectionNodes = node.getChildrenOfType(Constants.COLLECTION);
        if (collectionNodes != null && collectionNodes.size() > 0)
        {
        	subList = list.addList("sub-list-"+dso.getID());
        
            for (TreeNode collectionNode : collectionNodes)
            {
                String collectionName = ((Collection) collectionNode.getDSO()).getName();
                String collectionUrl = contextPath + "/handle/"+collectionNode.getDSO().getHandle();
                subList.addItemXref(collectionUrl, collectionName);
            }
        }

        // Add all the sub-communities
        java.util.List<TreeNode> communityNodes = node.getChildrenOfType(Constants.COMMUNITY);
        if (communityNodes != null && communityNodes.size() > 0)
        {
        	if (subList == null)
            {
                subList = list.addList("sub-list-" + dso.getID());
            }
            
            for (TreeNode communityNode : communityNodes)
            {
                buildList(subList,communityNode);
            }
        }
    }
    
    @Override
    public void recycle() 
    {
        this.root = null;
        this.validity = null;
        super.recycle();
    }

    /**
     * construct a tree structure of communities and collections. The results 
     * of this hierarchy are cached so calling it multiple times is acceptable.
     * 
     * @param communities The root level communities
     * @return A root level node.
     */
    private TreeNode buildTree(java.util.List<Community> communities) throws SQLException
    {
        if (root != null)
        {
            return root;
        }
        
        TreeNode newRoot = new TreeNode();

        // Setup for breadth-first traversal
        Stack<TreeNode> stack = new Stack<>();

        for (Community community : communities)
        {
            stack.push(newRoot.addChild(community));
        }

        while (!stack.empty())
        {
            TreeNode node = stack.pop();

            // Short-circuit if we have reached our max depth.
            if (node.getLevel() >= this.depth)
            {
                continue;
            }

            // Only communities nodes are pushed on the stack.
            Community community = (Community) node.getDSO();

            for (Community subcommunity : community.getSubcommunities())
            {
                stack.push(node.addChild(subcommunity));
            }

            // Add any collections to the document.
            if (!excludeCollections)
            {
                for (Collection collection : community.getCollections())
                {
                    node.addChild(collection);
                }
            }
        }
        
        this.root = newRoot;
        return root;
    }

    /**
     * Private class to represent the tree structure of communities and collections.
     */
    protected static class TreeNode
    {
        /** The object this node represents */
        private DSpaceObject dso;

        /** The level in the hierarchy that this node is at. */
        private int level;

        /** All children of this node */
        private final java.util.List<TreeNode> children = new ArrayList<>();

        /** 
         * Construct a new root level node 
         */
        public TreeNode()
        {
            // Root level node is add the zero level.
            this.level = 0;
        }

        /**
         * @return The DSpaceObject this node represents
         */
        public DSpaceObject getDSO()
        {
            return this.dso;
        }

        /**
         * Add a child DSpaceObject
         * 
         * @param dso The child
         * @return A new TreeNode object attached to the tree structure.
         */
        public TreeNode addChild(DSpaceObject dso)
        {
            TreeNode child = new TreeNode();
            child.dso = dso;
            child.level = this.level + 1;
            children.add(child);
            return child;
        }

        /**
         * @return The current level in the hierarchy of this node.
         */
        public int getLevel()
        {
            return this.level;
        }

        /**
         * @return All children
         */
        public java.util.List<TreeNode> getChildren()
        {
            return children;
        }

        /**
         * @param type interesting type.
         * @return All children of the given @type.
         */
        public java.util.List<TreeNode> getChildrenOfType(int type)
        {
            java.util.List<TreeNode> results = new ArrayList<>();
            for (TreeNode node : children)
            {
                if (node.dso.getType() == type)
                {
                    results.add(node);
                }
            }
            return results;
        }
    }

}
