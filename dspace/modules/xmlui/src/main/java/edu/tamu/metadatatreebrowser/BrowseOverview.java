package edu.tamu.metadatatreebrowser;

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
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.xml.sax.SAXException;

/**
 * Display entire metadata browse tree in one nested list, typically for the community or collection homepage.s
 * 
 * @author Scott Phillips, http://www.scottphillips.com
 * @author Jason Savell <jsavell@library.tamu.edu>
 * 
 * TODO Leverage Cocoon's caching mechanism
 */

public class BrowseOverview extends AbstractDSpaceTransformer implements
		CacheableProcessingComponent {

	/** Cached validity object */
	private SourceValidity validity;
	
	private MetadataTreeNode root;
	/** The age of the metadat tree */
	private Long rootAge;
	/** Life of the root cache in minutes **/
	private	Long rootLife = 480L;
	
	/**
	 * Generate the unique caching key.
	 */
	public Serializable getKey() {
		try {
			DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

			if (dso == null) {
				return "0"; // no item, something is wrong
			}

			return HashUtil.hash(dso.getHandle());
		} catch (SQLException sqle) {
			// Ignore all errors and just return that the component is not
			// cachable.
			return "0";
		}
	}

	/**
	 * Generate the cache validity object.
	 */
	public SourceValidity getValidity() {
		if (this.validity == null) {
			this.validity = new DSpaceValidity().complete();
		}
		return this.validity;
	}

	/**
	 * Display an overview of the browse tree for community or collection
	 * homepage.
	 */
	public void addBody(Body body) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException {

		DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
		
		Long now = System.currentTimeMillis() / 1000L;
		
		if (root == null || (now > (rootAge+(rootLife*60)))) {
			rootAge = now;
			root = MetadataTreeNode.generateBrowseTree(context, dso);
		}

		String baseURL = contextPath + "/handle/" + dso.getHandle()+ "/mdbrowse";

		Division div = body.addDivision("metadata-tree-browser-overview");
		div.setHead("Browse Sets");
		List rootList = div.addList("overview-list",List.TYPE_SIMPLE,"root-list");
		
		displayBrowseTree(root, rootList, baseURL);


	}
	


	/**
	 * Recursively translate the metadataTree into a DRI list for display.
	 * 
	 * @param node
	 *            The node to translate.
	 * @param list
	 *            The list to modify
	 * @param baseURL
	 *            The base url for refrences to individual items.
	 */
	private void displayBrowseTree(MetadataTreeNode node, List list,
			String baseURL) throws WingException {

		for (MetadataTreeNode childNode : node.getChildren()) {

			Item item = list.addItem();
			item.addXref(baseURL + "?node=" + childNode.getId(),childNode.getName());
			if (childNode.hasChildren()) {
				List childList = list.addList("sub-list-" + childNode.getId(),List.TYPE_SIMPLE,"sub-list");
				displayBrowseTree(childNode, childList, baseURL);
			}
		}
	}
}
