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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
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
 * plugin.selfnamed.org.dspace.content.authority.ChoiceAuthority = \
 * org.dspace.content.authority.DSpaceControlledVocabulary
 * }
 *
 * It AUTOMATICALLY configures a plugin instance for each XML file in the
 * controlled vocabularies directory. The name of the plugin is the basename of
 * the file; e.g., {@code ${dspace.dir}/config/controlled-vocabularies/nsi.xml}
 * would generate a plugin called "nsi".
 *
 * Each configured plugin comes with three configuration options: {@code
 * vocabulary.plugin._plugin_.hierarchy.store = <true|false>
 * # Store entire hierarchy along with selected value. Default: TRUE
 * vocabulary.plugin._plugin_.hierarchy.suggest =
 * <true|false>  # Display entire hierarchy in the suggestion list.  Default: TRUE
 * vocabulary.plugin._plugin_.delimiter = "<string>"
 * # Delimiter to use when building hierarchy strings. Default: "::"
 * }
 *
 * @author Michael B. Klein
 */

public class DSpaceControlledVocabulary extends SelfNamedPlugin implements HierarchicalAuthority {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(DSpaceControlledVocabulary.class);
    protected static String xpathTemplate = "//node[contains(translate(@label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
        "'abcdefghijklmnopqrstuvwxyz'),'%s')]";
    protected static String idTemplate = "//node[@id = '%s']";
    protected static String idParentTemplate = "//node[@id = '%s']/parent::isComposedBy";
    protected static String rootTemplate = "/node";
    protected static String pluginNames[] = null;

    protected String vocabularyName = null;
    protected InputSource vocabulary = null;
    protected Boolean suggestHierarchy = false;
    protected Boolean storeHierarchy = false;
    protected Integer preloadLevel = 1;
    protected String hierarchyDelimiter = "::";
    protected String rootNodeId = "";

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
                                                           .getProperty(
                                                               "dspace.dir") + "/config/controlled-vocabularies/";
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
            preloadLevel = config.getIntProperty(configurationPrefix + ".hierarchy.preloadLevel", preloadLevel);
            String configuredDelimiter = config.getProperty(configurationPrefix + ".delimiter");
            if (configuredDelimiter != null) {
                hierarchyDelimiter = configuredDelimiter.replaceAll("(^\"|\"$)", "");
            }
            String filename = vocabulariesPath + vocabularyName + ".xml";
            log.info("Loading " + filename);
            vocabulary = new InputSource(filename);

            XPath xpath = XPathFactory.newInstance().newXPath();
            try {
                Node rootNode = (Node) xpath.evaluate(rootTemplate, vocabulary, XPathConstants.NODE);
                Node idAttr = rootNode.getAttributes().getNamedItem("id");
                if (null != idAttr) { // 'id' is optional
                    rootNodeId = idAttr.getNodeValue();
                }
            } catch (XPathExpressionException e) {
                log.warn(e.getMessage(), e);
            }
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
        String xpathExpression = "";
        String[] textHierarchy = text.split(hierarchyDelimiter, -1);
        for (int i = 0; i < textHierarchy.length; i++) {
            xpathExpression += String.format(xpathTemplate, textHierarchy[i].replaceAll("'", "&apos;").toLowerCase());
        }
        XPath xpath = XPathFactory.newInstance().newXPath();
        List<Choice> choices = new ArrayList<Choice>();
        try {
            NodeList results = (NodeList) xpath.evaluate(xpathExpression, vocabulary, XPathConstants.NODESET);
            String[] authorities = new String[results.getLength()];
            String[] values = new String[results.getLength()];
            String[] labels = new String[results.getLength()];
            String[] parent = new String[results.getLength()];
            Boolean[] selectable = new Boolean[results.getLength()];
            ArrayList<String>[] children = new ArrayList[results.getLength()];
            String[] notes = new String[results.getLength()];
            for (int i = 0; i < results.getLength(); i++) {
                Node node = results.item(i);
                children[i] = new ArrayList<String>();
                readNode(authorities, values, labels, parent, children[i], notes, selectable, i, node);
            }
            int resultCount = labels.length - start;
            // limit = 0 means no limit
            if ((collection == null) && (limit > 0) && (resultCount > limit)) {
                resultCount = limit;
            }

            if (resultCount > 0) {
                for (int i = 0; i < resultCount; i++) {
                    Choice choice = new Choice(authorities[start + i], values[start + i], labels[start + i],
                            selectable[start + i]);
                    if (StringUtils.isNotBlank(parent[i])) {
                        choice.extras.put("parent", parent[i]);
                    }
                    if (StringUtils.isNotBlank(notes[i])) {
                        choice.extras.put("note", notes[i]);
                    }
                    if (children[i].size() > 0) {
                        choice.extras.put("children", String.join("::", children[i]));
                    }
                    choices.add(choice);
                }
            }
        } catch (XPathExpressionException e) {
            log.warn(e.getMessage(), e);
        }
        return new Choices(choices.toArray(new Choice[choices.size()]), 0, choices.size(), Choices.CF_AMBIGUOUS, false);
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
    public Choice getChoice(String fieldKey, String authKey, String locale) {
        init();
        log.debug("Getting matches for '" + authKey + "'");
        String xpathExpression = String.format(idTemplate, authKey);
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            Node node = (Node) xpath.evaluate(xpathExpression, vocabulary, XPathConstants.NODE);
            if (node != null) {
                return createChoiceFromNode(node);
            }
        } catch (XPathExpressionException e) {
            log.warn(e.getMessage(), e);
        }
        return null;
    }

    private void readNode(String[] authorities, String[] values, String[] labels, String[] parent,
            List<String> children, String[] notes, Boolean[] selectable, int i, Node node) {
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

        Node selectableAttr = node.getAttributes().getNamedItem("selectable");
        if (null != selectableAttr) {
            selectable[i] = Boolean.valueOf(selectableAttr.getNodeValue());
        } else { // Default is true
            selectable[i] = true;
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
                        if (null != parentIdAttr && !parentIdAttr.getNodeValue().equals(rootNodeId)) {
                            parent[i] = parentIdAttr.getNodeValue();
                        }
                    }
                }

                for (int ci = 0; ci < childNodes.getLength(); ci++) {
                    Node firstChild = childNodes.item(ci);
                    if (firstChild != null && "isComposedBy".equals(firstChild.getNodeName())) {
                        for (int cii = 0; cii < firstChild.getChildNodes().getLength(); cii++) {
                            Node childN = firstChild.getChildNodes().item(cii);
                            if (childN != null && "node".equals(childN.getNodeName())) {
                                Node childIdAttr = childN.getAttributes().getNamedItem("id");
                                if (null != childIdAttr) {
                                    children.add(childIdAttr.getNodeValue());
                                }
                            }
                        }
                        break;
                    }
                }
            }
        } else {
            authorities[i] = null;
            parent[i] = null;
        }
    }

    private Choice createChoiceFromNode(Node node) {
        if (node != null) {
            String[] authorities = new String[1];
            String[] values = new String[1];
            String[] labels = new String[1];
            String[] parent = new String[1];
            List<String> children = new ArrayList<String>();
            String[] note = new String[1];
            Boolean[] selectable = new Boolean[1];
            readNode(authorities, values, labels, parent, children, note, selectable, 0, node);

            if (values.length > 0) {
                Choice choice = new Choice(authorities[0], values[0], labels[0], selectable[0]);
                if (StringUtils.isNotBlank(parent[0])) {
                    choice.extras.put("parent", parent[0]);
                }
                if (StringUtils.isNotBlank(note[0])) {
                    choice.extras.put("note", note[0]);
                }
                if (children.size() > 0) {
                    choice.extras.put("children", String.join("::", children));
                }
                return choice;
            }
        }
        return new Choice("", "", "");
    }

    @Override
    public Choices getTopChoices(String authorityName, int start, int limit, String locale) {
        init();
        String xpathExpression = rootTemplate;
        List<Choice> choices = getChoicesByXpath(xpathExpression);

        return new Choices(choices.toArray(new Choice[choices.size()]), 0, choices.size(), Choices.CF_AMBIGUOUS, false);
    }

    @Override
    public Choices getChoicesByParent(String authorityName, String parentId, int start, int limit, String locale) {
        init();
        String xpathExpression = String.format(idTemplate, parentId);
        List<Choice> choices = getChoicesByXpath(xpathExpression);

        return new Choices(choices.toArray(new Choice[choices.size()]), 0, choices.size(), Choices.CF_AMBIGUOUS, false);
    }

    private List<Choice> getChoicesByXpath(String xpathExpression) {
        List<Choice> choices = new ArrayList<Choice>();
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            Node parentNode = (Node) xpath.evaluate(xpathExpression, vocabulary, XPathConstants.NODE);
            if (parentNode != null) {
                NodeList childNodes = (NodeList) xpath.evaluate(".//isComposedBy", parentNode, XPathConstants.NODE);
                if (null != childNodes) {
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        Node childNode = childNodes.item(i);
                        if (childNode != null && "node".equals(childNode.getNodeName())) {
                            choices.add(createChoiceFromNode(childNode));
                        }
                    }
                }
            }
        } catch (XPathExpressionException e) {
            log.warn(e.getMessage(), e);
        }
        return choices;
    }

    @Override
    public Integer getPreloadLevel() {
        return preloadLevel;
    }
}
