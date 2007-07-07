/*
 * CommunityBrowser.java
 *
 * Version: $Revision: 1.15 $
 *
 * Date: $Date: 2006/08/08 20:58:04 $
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
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;

import org.xml.sax.SAXException;

/**
 * Display a list of Communities and collections.
 * 
 * This item may be configured so that it will only display to a specific depth,
 * and may include or exclude collections from the tree.
 * 
 * The configuration option available: <depth exclude-collections="true">999</depth>
 * 
 * @author Scott Phillips
 */
public class CommunityBrowser extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
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

    /** Cached version the community / collection hierarch */
    protected TreeNode root;
    
    /** cached validity object */
    private SourceValidity validity;
    
    /**
     * Set the component up, pulling any configuration values from the sitemap
     * parameters.
     */
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, parameters);

        depth = parameters.getParameterAsInteger("depth", DEFAULT_DEPTH);
        excludeCollections = parameters.getParameterAsBoolean(
                "exclude-collections", false);
    }

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey()
    {
    	boolean full = ConfigurationManager.getBooleanProperty("xmlui.community-list.render.full", true);
        return HashUtil.hash(depth + "-" + excludeCollections + "-" + (full ? "true" : "false"));
    }

    /**
     * Generate the cache validity object.
     * 
     * The validity object will include a list of all communities 
     * & collection being browsed along with there logo bitstreams.
     */
    public SourceValidity getValidity()
    {
    	if (validity == null)
    	{
	        try {
	            DSpaceValidity validity = new DSpaceValidity();
	            
	            TreeNode root = buildTree(Community.findAllTop(context));
	            
	            Stack<TreeNode> stack = new Stack<TreeNode>();
	            stack.push(root);
	            
	            int objectCount = 0;
	            while (!stack.empty())
	            {
	            	objectCount++;
	                TreeNode node = stack.pop();
	                
	                validity.add(node.getDSO());
	                
	                for (TreeNode child : node.getChildren())
	                {
	                    stack.push(child);
	                }
	            }
	            
	            // Check if we are configured to assume validity.
	            String assumeCacheValidity = ConfigurationManager.getProperty("xmlui.community-list.cache");
	            if (assumeCacheValidity != null)
	            	validity.setAssumedValidityDelay(assumeCacheValidity);
	            
	            this.validity = validity.complete();
	        } 
	        catch (SQLException sqle) 
	        {
	            // ignore all errors and return an invalid cache.
	        }
    	}
    	return this.validity;
    }

    /**
     * Add a page title and trail links.
     */
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
     * Add a community-browser division that includes refrences to community and
     * collection metadata.
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Division division = body.addDivision("comunity-browser", "primary");
        division.setHead(T_head);
        division.addPara(T_select);

        TreeNode root = buildTree(Community.findAllTop(context));
        
        boolean full = ConfigurationManager.getBooleanProperty("xmlui.community-list.render.full", true);
        
        if (full)
        {
	        ReferenceSet referenceSet = division.addReferenceSet("community-browser",
	                ReferenceSet.TYPE_SUMMARY_LIST,null,"hierarchy");
	        
	        ArrayList<TreeNode> rootNodes = root.getChildrenOfType(Constants.COMMUNITY);
	        
	        for (TreeNode node : rootNodes)
	        {
	            buildReferenceSet(referenceSet,node);   
	        }
        }
        else
        {
        	List list = division.addList("comunity-browser");
        	
        	ArrayList<TreeNode> rootNodes = root.getChildrenOfType(Constants.COMMUNITY);
 	        
 	        for (TreeNode node : rootNodes)
 	        {
 	            buildList(list,node);   
 	        }
        	
        }
    } 
    
    /**
     * Recursively build an includeset of the community / collection hierarcher based upon
     * the given NodeTree.
     * 
     * @param referenceSet The include set
     * @param node The current node of the hierarch.
     */
    public void buildReferenceSet(ReferenceSet referenceSet, TreeNode node) throws WingException
    {
        DSpaceObject dso = node.getDSO();
        
        Reference objectInclude = referenceSet.addReference(dso);
        
        // Add all the sub-collections;
        ArrayList<TreeNode> collectionNodes = node.getChildrenOfType(Constants.COLLECTION);
        if (collectionNodes != null && collectionNodes.size() > 0)
        {
            ReferenceSet collectionSet = objectInclude.addReferenceSet(ReferenceSet.TYPE_SUMMARY_LIST);
            
            for (TreeNode collectionNode : collectionNodes)
            {
                collectionSet.addReference(collectionNode.getDSO());
            }
        }
        
        // Add all the sub-communities
        ArrayList<TreeNode> communityNodes = node.getChildrenOfType(Constants.COMMUNITY);
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
     * Recursively build a list of the community / collection hierarchery based upon
     * the given NodeTree.
     * 
     * @param List The parent list
     * @param node The current node of the hierarch.
     */
    public void buildList(List list, TreeNode node) throws WingException
    {
        DSpaceObject dso = node.getDSO();
        
        String name = null;
        if (dso instanceof Community)
        	name = ((Community) dso).getMetadata("name");
        else if (dso instanceof Collection)
        	name = ((Collection) dso).getMetadata("name");
        
        String url = contextPath + "/handle/"+dso.getHandle();
        list.addItem().addHighlight("bold").addXref(url, name);
        
        List subList = null;
        
        // Add all the sub-collections;
        ArrayList<TreeNode> collectionNodes = node.getChildrenOfType(Constants.COLLECTION);
        if (collectionNodes != null && collectionNodes.size() > 0)
        {
        	subList = list.addList("sub-list-"+dso.getID());
        
            for (TreeNode collectionNode : collectionNodes)
            {
                String collectionName = ((Collection) collectionNode.getDSO()).getMetadata("name");
                String collectionUrl = contextPath + "/handle/"+collectionNode.getDSO().getHandle();
                subList.addItemXref(collectionUrl, collectionName);
            }
        }
        
        
        // Add all the sub-communities
        ArrayList<TreeNode> communityNodes = node.getChildrenOfType(Constants.COMMUNITY);
        if (communityNodes != null && communityNodes.size() > 0)
        {
        	if (subList == null)
        		subList = list.addList("sub-list-"+dso.getID());
            
            for (TreeNode communityNode : communityNodes)
            {
                buildList(subList,communityNode);
            }
        }
    }
    
    /**
     * recycle
     */
    public void recycle() 
    {
        this.root = null;
        this.validity = null;
        super.recycle();
    }

    /**
     * construct a tree structure of communities and collections. The results 
     * of this hirarchy are cached so calling it multipletimes is acceptable.
     * 
     * @param communities The root level communities
     * @return A root level node.
     */
    private TreeNode buildTree(Community[] communities) throws SQLException
    {
        if (root != null)
            return root;
        
        TreeNode newRoot = new TreeNode();

        // Setup for breath-first traversal
        Stack<TreeNode> stack = new Stack<TreeNode>();

        for (Community community : communities)
            stack.push(newRoot.addChild(community));

        while (!stack.empty())
        {
            TreeNode node = stack.pop();

            // Short circuit if we have reached our max depth.
            if (node.getLevel() >= this.depth)
                continue;

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
     * Private class to represent the tree structure of communities & collections. 
     */
    protected static class TreeNode
    {
        /** The object this node represents */
        private DSpaceObject dso;

        /** The level in the hirarchy that this node is at. */
        private int level;

        /** All children of this node */
        private ArrayList<TreeNode> children = new ArrayList<TreeNode>();

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
         * @return The current level in the hirarchy of this node.
         */
        public int getLevel()
        {
            return this.level;
        }

        /**
         * @return All children
         */
        public ArrayList<TreeNode> getChildren()
        {
            return children;
        }

        /**
         * @return All children of the given @type.
         */
        public ArrayList<TreeNode> getChildrenOfType(int type)
        {
            ArrayList<TreeNode> results = new ArrayList<TreeNode>();
            for (TreeNode node : children)
            {
                if (node.dso.getType() == type)
                    results.add(node);
            }
            return results;
        }
    }

}
