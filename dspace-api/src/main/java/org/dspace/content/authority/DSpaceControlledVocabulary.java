/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * ChoiceAuthority source that reads the JSPUI-style hierarchical vocabularies
 * from {@code ${dspace.dir}/config/controlled-vocabularies/*.xml} and turns
 * them into autocompleting authorities.
 *
 * Configuration: This MUST be configured as a self-named plugin, e.g.: {@code
 *     plugin.selfnamed.org.dspace.content.authority.ChoiceAuthority = \
 *        org.dspace.content.authority.DSpaceControlledVocabulary
 *   }
 *
 * It AUTOMATICALLY configures a plugin instance for each XML file in the
 * controlled vocabularies directory. The name of the plugin is the basename of
 * the file; e.g., {@code ${dspace.dir}/config/controlled-vocabularies/nsi.xml}
 * would generate a plugin called "nsi".
 *
 * Each configured plugin comes with three configuration options: {@code
 *   vocabulary.plugin._plugin_.hierarchy.store = <true|false>
    # Store entire hierarchy along with selected value. Default: TRUE
 *   vocabulary.plugin._plugin_.hierarchy.suggest = 
<true|false>  # Display entire hierarchy in the suggestion list.  Default: TRUE
 *   vocabulary.plugin._plugin_.delimiter = "<string>"
              # Delimiter to use when building hierarchy strings. Default: "::"
 *  }
 *
 * @author Michael B. Klein
 *
 */

public class DSpaceControlledVocabulary extends SelfNamedPlugin implements ChoiceAuthority {

	private static Logger log = Logger.getLogger(DSpaceControlledVocabulary.class);
	protected static String xpathTemplate = "//node[contains(translate(@label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'%s')]";
	protected static String idTemplate = "//node[@id = '%s']";
	protected static String idParentTemplate = "//node[@id = '%s']/parent::isComposedBy";
	protected static String pluginNames[] = null;

	protected String vocabularyName = null;
	protected InputSource vocabulary = null;
	protected Boolean suggestHierarchy = true;
	protected Boolean storeHierarchy = true;
	protected String hierarchyDelimiter = "::";

	public DSpaceControlledVocabulary() {
		super();
	}

	public static String[] getPluginNames() {
		if (pluginNames == null) {
			initPluginNames();
		}

		return (String[]) ArrayUtils.clone(pluginNames);
	}

	private static synchronized void initPluginNames() {
		if (pluginNames == null) {
			class xmlFilter implements java.io.FilenameFilter {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			}
			String vocabulariesPath = DSpaceServicesFactory.getInstance().getConfigurationService()
					.getProperty("dspace.dir") + "/config/controlled-vocabularies/";
			String[] xmlFiles = (new File(vocabulariesPath)).list(new xmlFilter());
			List<String> names = new ArrayList<String>();
			for (String filename : xmlFiles) {
				names.add((new File(filename)).getName().replace(".xml", ""));
			}
			pluginNames = names.toArray(new String[names.size()]);
			log.info("Got plugin names = " + Arrays.deepToString(pluginNames));
		}
	}

	protected void init() {
		if (vocabulary == null) {
			ConfigurationService config = DSpaceServicesFactory.getInstance().getConfigurationService();

			log.info("Initializing " + this.getClass().getName());
			vocabularyName = this.getPluginInstanceName();
			String vocabulariesPath = config.getProperty("dspace.dir") + "/config/controlled-vocabularies/";
			String configurationPrefix = "vocabulary.plugin." + vocabularyName;
			storeHierarchy = config.getBooleanProperty(configurationPrefix + ".hierarchy.store", storeHierarchy);
			suggestHierarchy = config.getBooleanProperty(configurationPrefix + ".hierarchy.suggest", suggestHierarchy);
			String configuredDelimiter = config.getProperty(configurationPrefix + ".delimiter");
			if (configuredDelimiter != null) {
				hierarchyDelimiter = configuredDelimiter.replaceAll("(^\"|\"$)", "");
			}
			String filename = vocabulariesPath + vocabularyName + ".xml";
			log.info("Loading " + filename);
			vocabulary = new InputSource(filename);
		}
	}

	protected String buildString(Node node) {
		if (node.getNodeType() == Node.DOCUMENT_NODE) {
			return ("");
		} else {
			String parentValue = buildString(node.getParentNode());
			Node currentLabel = node.getAttributes().getNamedItem("label");
			if (currentLabel != null) {
				String currentValue = currentLabel.getNodeValue();
				if (parentValue.equals("")) {
					return currentValue;
				} else {
					return (parentValue + this.hierarchyDelimiter + currentValue);
				}
			} else {
				return (parentValue);
			}
		}
	}

	@Override
	public Choices getMatches(String field, String text, Collection collection, int start, int limit, String locale) {
		init();
		log.debug("Getting matches for '" + text + "'");
		String xpathExpression = String.format(xpathTemplate, text.replaceAll("'", "&apos;").toLowerCase());
		XPath xpath = XPathFactory.newInstance().newXPath();
		Choice[] choices;
		try {
			NodeList results = (NodeList) xpath.evaluate(xpathExpression, vocabulary, XPathConstants.NODESET);
			String[] authorities = new String[results.getLength()];
			String[] values = new String[results.getLength()];
			String[] labels = new String[results.getLength()];
			String[] parent = new String[results.getLength()];
			String[] notes = new String[results.getLength()];
			for (int i = 0; i < results.getLength(); i++) {
				Node node = results.item(i);
				readNode(authorities, values, labels, parent, notes, i, node);
			}
			int resultCount = labels.length - start;
			if ((limit > 0) && (resultCount > limit)) // limit = 0 means no
														// limit
				resultCount = limit;
			choices = new Choice[resultCount];
			if (resultCount > 0) {
				for (int i = 0; i < resultCount; i++) {
					choices[i] = new Choice(authorities[start + i], values[start + i], labels[start + i]);
					if (StringUtils.isNotBlank(parent[i])) {
						choices[i].extras.put("parent", parent[i]);
					}
					if (StringUtils.isNotBlank(notes[i])) {
						choices[i].extras.put("note", notes[i]);
					}
				}
			}
		} catch (XPathExpressionException e) {
			choices = new Choice[0];
		}
		return new Choices(choices, 0, choices.length, Choices.CF_AMBIGUOUS, false);
	}

	@Override
	public Choices getBestMatch(String field, String text, Collection collection, String locale) {
		init();
		log.debug("Getting best match for '" + text + "'");
		return getMatches(field, text, collection, 0, 2, locale);
	}

	@Override
	public String getLabel(String field, String key, String locale) {
		init();
		String xpathExpression = String.format(idTemplate, key);
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			Node node = (Node) xpath.evaluate(xpathExpression, vocabulary, XPathConstants.NODE);
			return node.getAttributes().getNamedItem("label").getNodeValue();
		} catch (XPathExpressionException e) {
			return ("");
		}
	}

	@Override
	public boolean isHierarchical() {
		return true;
	}

	@Override
	public Choice getChoice(String fieldKey, String authKey, String locale) {
		init();
		log.debug("Getting matches for '" + authKey + "'");
		String xpathExpression = String.format(idTemplate, authKey);
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			Node node = (Node) xpath.evaluate(xpathExpression, vocabulary, XPathConstants.NODE);
			if (node != null) {
				String[] authorities = new String[1];
				String[] values = new String[1];
				String[] labels = new String[1];
				String[] parent = new String[1];
				String[] note = new String[1];
				readNode(authorities, values, labels, parent, note, 0, node);

				if (values.length > 0) {
					Choice choice = new Choice(authorities[0], values[0], labels[0]);
					if (StringUtils.isNotBlank(parent[0])) {
						choice.extras.put("parent", parent[0]);
					}
					if (StringUtils.isNotBlank(note[0])) {
						choice.extras.put("note", note[0]);
					}
					return choice;
				}
			}
		} catch (XPathExpressionException e) {
			log.warn(e.getMessage(), e);
		}
		return new Choice("", "", "");
	}

	private void readNode(String[] authorities, String[] values, String[] labels, String[] parent, String[] notes,
			int i, Node node) {
		String hierarchy = this.buildString(node);
		if (this.suggestHierarchy) {
			labels[i] = hierarchy;
		} else {
			labels[i] = node.getAttributes().getNamedItem("label").getNodeValue();
		}
		if (this.storeHierarchy) {
			values[i] = hierarchy;
		} else {
			values[i] = node.getAttributes().getNamedItem("label").getNodeValue();
		}

		NodeList childNodes = node.getChildNodes();
		for (int ci = 0; ci < childNodes.getLength(); ci++) {
			Node firstChild = childNodes.item(ci);
			if (firstChild != null && "hasNote".equals(firstChild.getNodeName())) {
				String nodeValue = firstChild.getTextContent();
				if (StringUtils.isNotBlank(nodeValue)) {
					notes[i] = nodeValue;
				}
			}
		}
		Node idAttr = node.getAttributes().getNamedItem("id");
		if (null != idAttr) { // 'id' is optional
			authorities[i] = idAttr.getNodeValue();
			if (isHierarchical()) {
				Node parentN = node.getParentNode();
				if (parentN != null) {
					parentN = parentN.getParentNode();
					if (parentN != null) {
						Node parentIdAttr = parentN.getAttributes().getNamedItem("id");
						if (null != parentIdAttr) {
							parent[i] = parentIdAttr.getNodeValue();
						}
					}
				}
			}
		} else {
			authorities[i] = null;
			parent[i] = null;
		}
	}

}
