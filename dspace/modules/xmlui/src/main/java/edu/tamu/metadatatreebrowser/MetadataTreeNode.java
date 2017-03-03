package edu.tamu.metadatatreebrowser;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

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
 */
public class MetadataTreeNode {

    private static Logger log = Logger.getLogger(MetadataTreeNode.class);
	
	/** Member Variables **/
	private int node_id;
	private String name; // e.g. TAMU Organizations
	private String fieldValue; // e.g. TAMU Organizations, Historical Images of Texas A&M University
	private int thumbnail_id;
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
	private MetadataTreeNode(MetadataTreeNode parent, String name, int thumbnail_id,  int node_id) {
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
	
	
	public int getThumbnailId() {
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
		// We only apply to communities or collections. 
		if ( !(dso instanceof Collection || dso instanceof Community))
			return null;
		
		// Step 1: Get our config and determine if we should build a tree
		String handle = dso.getHandle();
		String fieldLabel = ConfigurationManager.getProperty("xmlui.mdbrowser."+handle+".field");
		String separator = ConfigurationManager.getProperty("xmlui.mdbrowser."+handle+".separator");
		boolean reverse = ConfigurationManager.getBooleanProperty("xmlui.mdbrowser."+handle+".reverse",false);
	
		if (fieldLabel == null || fieldLabel.length() == 0)
			return null;
		
		if (separator == null || separator.length() == 0)
			separator = ",";
		
		String[] fieldParts = fieldLabel.split("\\.");
		String schemaPart = fieldParts[0];
		String elementPart = fieldParts[1];
		String qualifierPart = null;
		if (fieldParts.length > 2)
			qualifierPart = fieldParts[2];
		
		MetadataSchema schema = MetadataSchema.find(context, schemaPart);
		MetadataField field = MetadataField.findByElement(context, schema.getSchemaID(), elementPart, qualifierPart);
		int fieldId = field.getFieldID();
		int dsoId = dso.getID();
		
		// Step 2: Query for the complete list of metadata elements, and process each one.
		MetadataTreeNode root = new MetadataTreeNode();
		int currentID = 1;
		
		// Find all distinct text_values for the metadata field in the given collection. And select 
		String query = "SELECT mv.text_value, MAX(b2b.bitstream_id) AS bitstream_id "
			+ "FROM metadatavalue mv, item i, collection2item c2i, item2bundle i2b, bundle b,metadatavalue tmv, bundle2bitstream b2b "
			+ "WHERE mv.resource_id = i.item_id "
			+ "AND i.item_id = c2i.item_id "
			+ "AND i.item_id = i2b.item_id "
			+ "AND i2b.bundle_id = b2b.bundle_id "
			+ "AND b2b.bundle_id = b.bundle_id "
			+ "AND (tmv.text_value = 'THUMBNAIL' AND b.bundle_id=tmv.resource_id)"
			+ "AND c2i.collection_id = ? "
			+ "AND mv.metadata_field_id = ? "
			+ "GROUP BY mv.text_value "
			+ "ORDER BY mv.text_value ";
		
		if (dso instanceof Community) {
			// The same query as above but searches through a community and all lower collections as well.
			query = "SELECT mv.text_value, max(b2b.bitstream_id) AS bitstream_id"
				+ "FROM metadatavalue mv, item i, collection2item c2i, community2collection c2c, item2bundle i2b, bundle b,metadatavalue tmv, bundle2bitstream b2b "
				+ "WHERE mv.resource_id = i.item_id "
				+ "AND i.item_id = c2i.item_id "
				+ "AND c2i.collection_id = c2c.collection_id "
				+ "AND i.item_id = i2b.item_id "
				+ "AND i2b.bundle_id = b2b.bundle_id "
				+ "AND b2b.bundle_id = b.bundle_id "
				+ "AND (tmv.text_value = 'THUMBNAIL' AND b.bundle_id=tmv.resource_id)"
				+ "AND c2c.community_id = ? "
				+ "AND mv.metadata_field_id = ? "
				+ "GROUP BY mv.text_value "
				+ "ORDER BY mv.text_value";
		}
		
		TableRowIterator rowItr = DatabaseManager.query(context, query, dsoId, fieldId);
		while (rowItr.hasNext()) {

			TableRow row = rowItr.next();
			String value = row.getStringColumn("text_value");
			int bitstream_id = row.getIntColumn("bitstream_id");
			String[] nameParts = value.split(separator);
			if (reverse)
				ArrayUtils.reverse(nameParts);
			
			// Iterate over the parts to establish all the parent nodes down to this leaf node.
			MetadataTreeNode parent = root;
			for (String namePart : nameParts) {
				MetadataTreeNode child = parent.getChild(namePart);
						
				if (child == null) {
					child = new MetadataTreeNode(parent,namePart, bitstream_id, currentID++);
				}
				parent = child;
			}
			
			// Set the leaf node to the exact path
			parent.setFieldValue(value);
		}
		
		return root;
	}
	
}
