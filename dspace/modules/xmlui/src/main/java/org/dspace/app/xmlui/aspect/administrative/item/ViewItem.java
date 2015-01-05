/*
 * EditItemStatus.java
 * 
 * Version: $Revision: 3705 $
 * 
 * Date: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
 * 
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts Institute of
 * Technology. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.StringTokenizer;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.PluginManager;
import org.dspace.handle.HandleManager;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

/**
 * Display basic meta-meta information about the item and allow the user to
 * change it's state such as withdraw or reinstate, possibily even completely
 * deleting the item!
 * 
 * @author Jay Paz
 * @author Scott Phillips
 */

@SuppressWarnings("deprecation")
public class ViewItem extends AbstractDSpaceTransformer {

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");

	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");

	private static final Message T_option_head = message("xmlui.administrative.item.general.option_head");

	private static final Message T_option_status = message("xmlui.administrative.item.general.option_status");

	private static final Message T_option_bitstreams = message("xmlui.administrative.item.general.option_bitstreams");

	private static final Message T_option_metadata = message("xmlui.administrative.item.general.option_metadata");

	private static final Message T_option_view = message("xmlui.administrative.item.general.option_view");

	private static final Message T_title = message("xmlui.administrative.item.ViewItem.title");

	private static final Message T_trail = message("xmlui.administrative.item.ViewItem.trail");

	private static final Message T_head_parent_collections = message("xmlui.ArtifactBrowser.ItemViewer.head_parent_collections");

	private static final Message T_show_simple = message("xmlui.ArtifactBrowser.ItemViewer.show_simple");

	private static final Message T_show_full = message("xmlui.ArtifactBrowser.ItemViewer.show_full");

    private static final Message T_option_curate = message("xmlui.administrative.item.general.option_curate");

    private static final Message T_option_embargo = message("xmlui.administrative.item.general.option_embargo");

	public void addPageMeta(PageMeta pageMeta) throws WingException,
			SQLException, AuthorizeException, IOException {
		StringBuilder buffer = new StringBuilder();
		boolean identifierSet = false;
		DCValue[] values;

		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item", T_item_trail);
		pageMeta.addTrail().addContent(T_trail);

		int itemID = parameters.getParameterAsInteger("itemID", -1);
		Item item = Item.find(context, itemID);

		if (item == null) return;

		String handle = item.getHandle();
		Item pkg = (Item) HandleManager.resolveToDataPackage(context, handle);

		// Find out whether our theme should be localized
		String localize = ConfigurationManager.getProperty("dryad.localize");

		if (localize != null && localize.equals("true")) {
			pageMeta.addMetadata("dryad", "localize").addContent("true");
		}

		if (pkg != null) {
			String pkgTitle = getItemTitle(pkg).trim();
			String author;

			for (DCValue pkgMeta : pkg.getMetadata("dc.identifier.citation")) {
				pageMeta.addMetadata("citation", "article").addContent(
						pkgMeta.value);
			}

			buffer.append(parseName(pkg.getMetadata("dc.contributor.author")));
			buffer.append(parseName(pkg.getMetadata("dc.creator")));
			buffer.append(parseName(pkg.getMetadata("dc.contributor")));

			author = buffer.toString().trim();
			author = author.endsWith(",") ? author.substring(0,
					author.length() - 1) : author;

			pageMeta.addMetadata("authors", "package").addContent(author + " ");
			pageMeta.addMetadata("title", "package").addContent(
					pkgTitle.endsWith(".") ? pkgTitle + " " : pkgTitle + ". ");

			if (pkg.getMetadata("dryad.curatorNotePublic") != null) {
				pageMeta.addMetadata("curatorNotePublic", "package").addContent(pkg.getMetadata("dryad.curatorNotePublic").toString());
			}
			if ((values = pkg.getMetadata("dc.date.issued")).length > 0) {
				pageMeta.addMetadata("dateIssued", "package").addContent(
						"(" + values[0].value.substring(0, 4) + ")");
			}

			if ((values = pkg.getMetadata("dc.relation.isreferencedby")).length != 0) {
				pageMeta.addMetadata("identifier", "article").addContent(
						values[0].value);
			}

			if ((values = pkg.getMetadata("prism.publicationName")).length != 0) {
				pageMeta.addMetadata("publicationName").addContent(
						values[0].value);
			}

			if ((values = pkg.getMetadata("dc.identifier")).length != 0) {
				for (DCValue value : values) {
					if (value.value.startsWith("doi:")) {
						pageMeta.addMetadata("identifier", "package")
								.addContent(value.value);
					}
				}
			}
			else if ((values = pkg.getMetadata("dc.identifier.uri")).length != 0) {
				for (DCValue value : values) {
					if (value.value.startsWith("doi:")) {
						pageMeta.addMetadata("identifier", "package")
								.addContent(value.value);
						identifierSet = true;
					}
				}

				if (!identifierSet) {
					for (DCValue value : values) {
						if (value.value.startsWith("http://dx.doi.org/")) {
							pageMeta.addMetadata("identifier", "package")
									.addContent(value.value.substring(18));
							identifierSet = true;
						}
					}
				}

				if (!identifierSet) {
					for (DCValue value : values) {
						if (value.value.startsWith("hdl:")) {
							pageMeta.addMetadata("identifier", "package")
									.addContent(value.value);
							identifierSet = true;
						}
					}
				}

				if (!identifierSet) {
					for (DCValue value : values) {
						if (value.value.startsWith("http://hdl.handle.net/")) {
							pageMeta.addMetadata("identifier", "package")
									.addContent(value.value.substring(22));
						}
					}
				}
			}
		}

		/** XHTML crosswalk instance */
		DisseminationCrosswalk xHTMLHeadCrosswalk = (DisseminationCrosswalk) PluginManager
				.getNamedPlugin(DisseminationCrosswalk.class, "XHTML_HEAD_ITEM");

		// Produce <meta> elements for header from crosswalk
		try {
			java.util.List l = xHTMLHeadCrosswalk.disseminateList(item);
			StringWriter sw = new StringWriter();

			XMLOutputter xmlo = new XMLOutputter();
			for (int i = 0; i < l.size(); i++) {
				Element e = (Element) l.get(i);
				// FIXME: we unset the Namespace so it's not printed.
				// This is fairly yucky, but means the same crosswalk should
				// work for Manakin as well as the JSP-based UI.
				e.setNamespace(null);
				xmlo.output(e, sw);
			}
			pageMeta.addMetadata("xhtml_head_item").addContent(sw.toString());
		}
		catch (CrosswalkException ce) {
			// TODO: Is this the right exception class?
			throw new WingException(ce);
		}
	}

	public void addBody(Body body) throws SQLException, WingException {
		// Get our parameters and state
		Request request = ObjectModelHelper.getRequest(objectModel);
		String show = request.getParameter("show");
		boolean showFullItem = false;
		if (show != null && show.length() > 0) showFullItem = true;

		int itemID = parameters.getParameterAsInteger("itemID", -1);
		Item item = Item.find(context, itemID);
		String baseURL = contextPath + "/admin/item?administrative-continue="
				+ knot.getId();

		String link = baseURL + "&view_item"
				+ (showFullItem ? "" : "&show=full");
		String tabLink = baseURL + "&view_item"
				+ (!showFullItem ? "" : "&show=full");
		// DIVISION: main
		Division main = body.addInteractiveDivision("edit-item-status",
				contextPath + "/admin/item", Division.METHOD_POST,
				"primary administrative edit-item-status");
		main.setHead(T_option_head);

		// LIST: options
		List options = main.addList("options", List.TYPE_SIMPLE, "horizontal");
		options.addItem().addXref(baseURL + "&submit_status", T_option_status);
		options.addItem().addXref(baseURL + "&submit_bitstreams",T_option_bitstreams);
        options.addItem().addXref(baseURL + "&submit_embargo", T_option_embargo);
		options.addItem().addXref(baseURL + "&submit_metadata",T_option_metadata);
		options.addItem().addHighlight("bold").addXref(tabLink, T_option_view);
        options.addItem().addXref(baseURL + "&submit_curate", T_option_curate);

		// item

		Para showfullPara = main.addPara(null,
				"item-view-toggle item-view-toggle-top");

		if (showFullItem) {
			link = baseURL + "&view_item";
			showfullPara.addXref(link).addContent(T_show_simple);
		}
		else {
			link = baseURL + "&view_item&show=full";
			showfullPara.addXref(link).addContent(T_show_full);
		}

		ReferenceSet referenceSet;
		referenceSet = main.addReferenceSet("collection-viewer",
				showFullItem ? ReferenceSet.TYPE_DETAIL_VIEW
						: ReferenceSet.TYPE_SUMMARY_VIEW);
		// Refrence the actual Item
		ReferenceSet appearsInclude = referenceSet.addReference(item)
				.addReferenceSet(ReferenceSet.TYPE_DETAIL_LIST, null,
						"hierarchy");
		appearsInclude.setHead(T_head_parent_collections);

		// Reference all collections the item appears in.
		for (Collection collection : item.getCollections()) {
			appearsInclude.addReference(collection);
		}

		showfullPara = main.addPara(null,
				"item-view-toggle item-view-toggle-bottom");

		if (showFullItem) {
			showfullPara.addXref(link).addContent(T_show_simple);
		}
		else {
			showfullPara.addXref(link).addContent(T_show_full);
		}
	}

	private String parseName(DCValue[] aMetadata) {
		StringBuilder buffer = new StringBuilder();
		int position = 0;

		for (DCValue metadata : aMetadata) {

			if (metadata.value.indexOf(",") != -1) {
				String[] parts = metadata.value.split(",");

				if (parts.length > 1) {
					StringTokenizer tokenizer = new StringTokenizer(parts[1],
							". ");

					buffer.append(parts[0]).append(" ");

					while (tokenizer.hasMoreTokens()) {
						buffer.append(tokenizer.nextToken().charAt(0));
					}
				}
			}
			else {
				// now the minority case (as we clean up the data)
				String[] parts = metadata.value.split("\\s+|\\.");
				String author = parts[parts.length - 1].replace("\\s+|\\.", "");
				char ch;

				buffer.append(author).append(" ");

				for (int index = 0; index < parts.length - 1; index++) {
					if (parts[index].length() > 0) {
						ch = parts[index].replace("\\s+|\\.", "").charAt(0);
						buffer.append(ch);
					}
				}
			}

			if (++position < aMetadata.length) {
				if (aMetadata.length > 2) {
					buffer.append(", ");
				}
				else {
					buffer.append(" and ");
				}
			}
		}

		return buffer.length() > 0 ? buffer.toString() : "";
	}

	/**
	 * Obtain the item's title.
	 */
	public static String getItemTitle(Item item) {
		DCValue[] titles = item.getDC("title", Item.ANY, Item.ANY);

		String title;
		if (titles != null && titles.length > 0) title = titles[0].value;
		else title = null;
		return title;
	}
}
