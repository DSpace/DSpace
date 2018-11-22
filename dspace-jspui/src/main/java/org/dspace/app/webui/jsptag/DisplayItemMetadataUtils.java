/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.log4j.Logger;
import org.dspace.app.util.MetadataExposure;
import org.dspace.app.webui.util.DateDisplayStrategy;
import org.dspace.app.webui.util.DefaultDisplayStrategy;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.app.webui.util.LinkDisplayStrategy;
import org.dspace.app.webui.util.ResolverDisplayStrategy;
import org.dspace.app.webui.util.StyleSelection;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.PluginManager;

public class DisplayItemMetadataUtils {
	/** log4j logger */
	private static Logger log = Logger.getLogger(ItemTag.class);

	/** Default DC fields to display, in absence of configuration */
	private static String defaultFields = "dc.title, dc.title.alternative, dc.contributor.*, dc.subject, dc.date.issued(date), dc.publisher, dc.identifier.citation, dc.relation.ispartofseries, dc.description.abstract, dc.description, dc.identifier.govdoc, dc.identifier.uri(link), dc.identifier.isbn, dc.identifier.issn, dc.identifier.ismn, dc.identifier";

	private static StyleSelection styleSelection = (StyleSelection) PluginManager.getSinglePlugin(StyleSelection.class);

	/** Hashmap of linked metadata to browse, from dspace.cfg */
	private static Map<String, String> linkedMetadata;

	/**
	 * regex pattern to capture the style of a field, ie
	 * <code>schema.element.qualifier(style)</code>
	 */
	private static Pattern fieldStylePatter = Pattern.compile(".*\\((.*)\\)");

	static {
		int i;

		linkedMetadata = new HashMap<String, String>();
		String linkMetadata;

		i = 1;
		do {
			linkMetadata = ConfigurationManager.getProperty("webui.browse.link." + i);
			if (linkMetadata != null) {
				String[] linkedMetadataSplit = linkMetadata.split(":");
				String indexName = linkedMetadataSplit[0].trim();
				String metadataName = linkedMetadataSplit[1].trim();
				linkedMetadata.put(indexName, metadataName);
			}

			i++;
		} while (linkMetadata != null);
	}

	/**
	 * Return the browse index related to the field. <code>null</code> if the
	 * field is not a browse field (look for <cod>webui.browse.link.<n></code>
	 * in dspace.cfg)
	 * 
	 * @param field
	 * @return the browse index related to the field. Null otherwise
	 * @throws BrowseException
	 */
	private static String getBrowseField(String field) throws BrowseException {
		for (String indexName : linkedMetadata.keySet()) {
			StringTokenizer bw_dcf = new StringTokenizer(linkedMetadata.get(indexName), ".");

			String[] bw_tokens = { "", "", "" };
			int i = 0;
			while (bw_dcf.hasMoreTokens()) {
				bw_tokens[i] = bw_dcf.nextToken().toLowerCase().trim();
				i++;
			}
			String bw_schema = bw_tokens[0];
			String bw_element = bw_tokens[1];
			String bw_qualifier = bw_tokens[2];

			StringTokenizer dcf = new StringTokenizer(field, ".");

			String[] tokens = { "", "", "" };
			int j = 0;
			while (dcf.hasMoreTokens()) {
				tokens[j] = dcf.nextToken().toLowerCase().trim();
				j++;
			}
			String schema = tokens[0];
			String element = tokens[1];
			String qualifier = tokens[2];
			if (schema.equals(bw_schema) // schema match
					&& element.equals(bw_element) // element match
					&& ((bw_qualifier != null)
							&& ((qualifier != null && qualifier.equals(bw_qualifier)) // both
																						// not
																						// null
																						// and
																						// equals
									|| bw_qualifier.equals("*")) // browse link
																	// with
																	// jolly
							|| (bw_qualifier == null && qualifier == null)) // both
																			// null
			) {
				return indexName;
			}
		}
		return null;
	}

	public static List<DisplayMetadata> getDisplayMetadata(Context context, HttpServletRequest req, Item item) throws SQLException, JspException {
		return getDisplayMetadata(context, req, item, null);
	}

	public static List<DisplayMetadata> getDisplayMetadata(Context context, HttpServletRequest req, Item item,
			String postfix) throws SQLException, JspException {
		List<DisplayMetadata> metadata = new ArrayList<DisplayMetadata>();

		String style = styleSelection.getStyleForItem(context, item, req);
		String configLine = "";
		if (postfix != null && styleSelection.isConfigurationDefinedForStyle(context, style + "." + postfix, req)) {
			configLine = styleSelection.getConfigurationForStyle(context, style + "." + postfix, req);
		} else {
			configLine = styleSelection.getConfigurationForStyle(context, style, req);
		}

		if (configLine == null) {
			configLine = defaultFields;
		}

		/*
		 * Break down the configuration into fields and display them
		 * 
		 * FIXME?: it may be more efficient to do some processing once, perhaps
		 * to a more efficient intermediate class, but then it would become more
		 * difficult to reload the configuration "on the fly".
		 */
		StringTokenizer st = new StringTokenizer(configLine, ",");

		while (st.hasMoreTokens()) {
			String field = st.nextToken().trim();
			String displayStrategyName = null;
			Matcher fieldStyleMatcher = fieldStylePatter.matcher(field);
			if (fieldStyleMatcher.matches()) {
				displayStrategyName = fieldStyleMatcher.group(1);
			}

			if (displayStrategyName != null) {
				field = field.replaceAll("\\(" + displayStrategyName + "\\)", "");
			} else {
				displayStrategyName = "default";
			}

			String browseIndex;
			boolean viewFull = false;
			try {
				browseIndex = getBrowseField(field);
				if (browseIndex != null) {
					viewFull = BrowseIndex.getBrowseIndex(browseIndex).isItemIndex();
				}
			} catch (BrowseException e) {
				log.error(e);
				browseIndex = null;
			}

			// Get the separate schema + element + qualifier

			String[] eq = field.split("\\.");
			String schema = eq[0];
			String element = eq[1];
			String qualifier = null;
			if (eq.length > 2 && eq[2].equals("*")) {
				qualifier = Item.ANY;
			} else if (eq.length > 2) {
				qualifier = eq[2];
			}

			// check for hidden field, even if it's configured..
			if (MetadataExposure.isHidden(context, schema, element, qualifier)) {
				continue;
			}

			// FIXME: Still need to fix for metadata language?
			Metadatum[] values = item.getMetadataWithoutPlaceholder(schema, element, qualifier, Item.ANY);

			if (values.length > 0) {

				String label = null;
				try {
					label = I18nUtil.getMessage("metadata." + ("default".equals(style) ? "" : style + ".") + field,
							context.getCurrentLocale(), true);
				} catch (MissingResourceException e) {
					// if there is not a specific translation for the style we
					// use the default one
					label = I18nUtil.getMessage("metadata." + field, context);
				}

				IDisplayMetadataValueStrategy strategy = (IDisplayMetadataValueStrategy) PluginManager
						.getNamedPlugin(IDisplayMetadataValueStrategy.class, displayStrategyName);

				if (strategy == null) {
					if (displayStrategyName.equalsIgnoreCase("link")) {
						strategy = new LinkDisplayStrategy();
					} else if (displayStrategyName.equalsIgnoreCase("date")) {
						strategy = new DateDisplayStrategy();
					} else if (displayStrategyName.equalsIgnoreCase("resolver")) {
						strategy = new ResolverDisplayStrategy();
					} else {
						strategy = new DefaultDisplayStrategy(displayStrategyName);						
					}
				}

				String displayvalue = strategy.getMetadataDisplay(req, -1, viewFull, browseIndex, -1, field, values,
						item, false, false);

				metadata.add(new DisplayMetadata(label, displayvalue));
			}
		}
		return metadata;
	}

	public static class DisplayMetadata {
		public String label;
		public String value;
		
		public DisplayMetadata(String label, String value) {
			this.label = label;
			this.value = value;
		}
	}
}