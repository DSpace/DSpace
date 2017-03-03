package edu.tamu.metadatatreebrowser;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * 
 * Metadata Tree node represents a browse node derived from a specific field of
 * a community or collection. The field must contain values seperated by a
 * common character, and when reconstituted from their leaf nodes form a
 * browsable hierarchy. For example if the fields contained the following
 * values:
 * 
 * A, B, C A, D A, B, E
 * 
 * The following tree would be produced:
 * 
 * A -> B,D B -> C,E
 * 
 * @author Scott Phillips, http://www.scottphillips.com/
 * @author Alexey Maslov
 * @author Jason Savell
 */
public class MetadataTreeNode {

	/** Member Variables **/
	private int node_id;
	private String name; // e.g. TAMU Organizations
	private String fieldValue; // e.g. TAMU Organizations, Historical Images of Texas A&M University
	private UUID thumbnail_id;
	private MetadataTreeNode parent;
	private List<MetadataTreeNode> children;
	
	
	/** Create a root node **/
	private MetadataTreeNode() {
		this.node_id = 0;
		this.name = null;
		this.fieldValue = null;
		this.parent = null;
		this.children = new ArrayList<MetadataTreeNode>();
	}
	
	/** Create a stem/content node **/
	private MetadataTreeNode(MetadataTreeNode parent, String name, UUID thumbnail_id,  int node_id) {
		this.node_id = node_id;
		this.parent = parent;
		this.name = name;
		this.fieldValue = null;
		this.thumbnail_id = thumbnail_id;
		this.children = new ArrayList<MetadataTreeNode>();
		this.parent.children.add(this);
	}
	
	public int getId() {
		return node_id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getFieldValue() {
		return fieldValue;
	}
	
	public void setFieldValue(String path) {
		this.fieldValue = path;
	}
	
	public MetadataTreeNode getParent() {
		return parent;
	}
	
	/**
	 * @return A list of all parents starting with the root, down to this node (this node is not included)
	 */
	public List<MetadataTreeNode> getParents() {
		
		ArrayList<MetadataTreeNode> parents = new ArrayList<MetadataTreeNode>();
		
		MetadataTreeNode node = parent;
		while (node != null) {
			parents.add(0,node);
			node = node.parent;
		}
		return parents;
	}
	
	public List<MetadataTreeNode> getChildren() {
		return children;
	}
	
	public MetadataTreeNode getChild(String name) {
		for (MetadataTreeNode child : children) {
			if (name.equals(child.name))
				return child;
		}
		return null;
	}
	
	public boolean hasChildren() {
		return children.size() == 0 ? false : true;
	}
	
	public boolean hasContent() {
		return fieldValue != null;
	}
	
	public boolean isRoot() {
		return name == null;
	}
	
	// Depth first
	public MetadataTreeNode findById(int node_id) {
		
		if (this.node_id == node_id)
			return this;
		
		for (MetadataTreeNode child : children) {
			if (child.node_id == node_id)
				return child;
			
			MetadataTreeNode found = child.findById(node_id);
			if (found != null)
				return found;
		}
		
		// nothing found
		return null;
	}
	
	
	public UUID getThumbnailId() {
		return thumbnail_id;
	}
	
	
	
	
	/**
	 * Generate browse tree from a set of metadata fields on a collection. This
	 * will check the dspace.cfg configuration to determine if this collection
	 * is setup for metadata browsing. It is safe to call this in either
	 * condition, null will be returned if the collection is not configured for
	 * metadata browsing.
	 * 
	 * @param context
	 *            The Dspace context.
	 * @param collection
	 *            The collection to browse.
	 * @return A root node for the browse tree, or null if not configured.
	 */
	public static MetadataTreeNode generateBrowseTree(Context context, DSpaceObject dso) throws SQLException, AuthorizeException {
	    ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
	    BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

		// We only apply to communities or collections. 
		if ( !(dso instanceof Collection || dso instanceof Community))
			return null;
		
		// Step 1: Get our config and determine if we should build a tree
		String handle = dso.getHandle();

		String fieldLabel = configurationService.getProperty("xmlui.mdbrowser."+handle+".field");
		String separator = configurationService.getProperty("xmlui.mdbrowser."+handle+".separator");
		boolean reverse = configurationService.getBooleanProperty("xmlui.mdbrowser."+handle+".reverse",false);
	
		if (fieldLabel == null || fieldLabel.length() == 0)
			return null;
		
		if (separator == null || separator.length() == 0)
			separator = ",";
		
		// Step 2: Query for the complete list of metadata elements, and process each one.
		MetadataTreeNode root = new MetadataTreeNode();
		int currentID = 1;

		Iterator<Bitstream> bitstreamsIterator = null;
		if (dso instanceof Community) {
			bitstreamsIterator = bitstreamService.getCommunityBitstreams(context, (Community) dso);
		} else {
			bitstreamsIterator = bitstreamService.getCollectionBitstreams(context, (Collection) dso);
		}

		while (bitstreamsIterator.hasNext()) {
			Bitstream bitstream = bitstreamsIterator.next();
			Iterator<Bundle> bundleIterator = bitstream.getBundles().iterator();
			while (bundleIterator.hasNext()) {
				Bundle bundle = bundleIterator.next();
				if (bundle.getName().equals("THUMBNAIL")) {
					Iterator<Item> itemsIterator = bundle.getItems().iterator();
					while (itemsIterator.hasNext()) {
						Item item = itemsIterator.next();
						String nameHierarchy = item.getItemService().getMetadata(item, fieldLabel);
						String[] nameParts = nameHierarchy.split(separator);
						// Iterate over the parts to establish all the parent nodes down to this leaf node.
						MetadataTreeNode parent = root;
						for (String namePart : nameParts) {
							MetadataTreeNode child = parent.getChild(namePart);
									
							if (child == null) {
								child = new MetadataTreeNode(parent,namePart, bitstream.getID(), currentID++);
							}
							parent = child;
						}
					}
				}
			}
		}
		return root;
	}
	
}
