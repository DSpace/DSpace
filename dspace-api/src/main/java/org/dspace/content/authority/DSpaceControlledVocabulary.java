/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * ChoiceAuthority source that reads the hierarchical vocabularies
 * from {@code ${dspace.dir}/config/controlled-vocabularies/*.xml} and turns
 * them into autocompleting authorities.
 *
 * <p>Configuration: This MUST be configured as a self-named plugin, e.g.: {@code
 * plugin.selfnamed.org.dspace.content.authority.ChoiceAuthority =
 * org.dspace.content.authority.DSpaceControlledVocabulary
 * }
 *
 * <p>It AUTOMATICALLY configures a plugin instance for each XML file in the
 * controlled vocabularies directory. The name of the plugin is the basename of
 * the file; e.g., {@code ${dspace.dir}/config/controlled-vocabularies/nsi.xml}
 * would generate a plugin called "nsi".
 *
 * <p>Each configured plugin comes with three configuration options:
 * <ul>
 *  <li>{@code vocabulary.plugin._plugin_.hierarchy.store = <true|false>
 * # Store entire hierarchy along with selected value. Default: TRUE}</li>
 *  <li>{@code vocabulary.plugin._plugin_.hierarchy.suggest =
 * <true|false>  # Display entire hierarchy in the suggestion list.  Default: TRUE}</li>
 *  <li>{@code vocabulary.plugin._plugin_.delimiter = "<string>"
 * # Delimiter to use when building hierarchy strings. Default: "::"}</li>
 * </ul>
 * }
 *
 * @author Michael B. Klein
 */

public class DSpaceControlledVocabulary extends SelfNamedPlugin implements HierarchicalAuthority {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();
    protected static String xpathTemplate = "//node[contains(translate(@label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
        "'abcdefghijklmnopqrstuvwxyz'),%s)]";
    protected static String idTemplate = "//node[@id = %s]";
    protected static String idTemplateQuoted = "//node[@id = '%s']";
    protected static String labelTemplate = "//node[@label = %s]";
    protected static String idParentTemplate = "//node[@id = '%s']/parent::isComposedBy/parent::node";
    protected static String rootTemplate = "/node";
    protected static String idAttribute = "id";
    protected static String labelAttribute = "label";
    protected static String[] pluginNames = null;
    public static final String ID_SPLITTER = ":";
    protected String vocabularyName = null;
    protected InputSource vocabulary = null;
    protected Boolean suggestHierarchy = false;
    protected Boolean storeHierarchy = true;
    protected String hierarchyDelimiter = "::";
    protected Integer preloadLevel = 1;
    protected String valueAttribute = labelAttribute;
    protected String valueTemplate = labelTemplate;

    public DSpaceControlledVocabulary() {
        super();
    }

    @Override
    public boolean storeAuthorityInMetadata() {
        // For backward compatibility controlled vocabularies don't store the node id in
        // the metadatavalue
        return false;
    }

    public static String[] getPluginNames() {
        if (pluginNames == null) {
            initPluginNames();
        }

        return ArrayUtils.clone(pluginNames);
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
                                                           .getProperty("dspace.dir") +
                File.separator + "config" +
                File.separator + "controlled-vocabularies";
            String[] xmlFiles = (new File(vocabulariesPath)).list(new xmlFilter());
            List<String> names = new ArrayList<>();
            for (String filename : xmlFiles) {
                names.add((new File(filename)).getName().replace(".xml", ""));
            }
            pluginNames = names.toArray(new String[names.size()]);
            log.info("Got plugin names = " + Arrays.deepToString(pluginNames));
        }
    }

    protected void init(String locale) {
        if (vocabulary == null) {
            ConfigurationService config = DSpaceServicesFactory.getInstance().getConfigurationService();

            log.info("Initializing " + this.getClass().getName());
            vocabularyName = this.getPluginInstanceName();
            String vocabulariesPath = config.getProperty("dspace.dir") + File.separator + "config" +
                File.separator + "controlled-vocabularies" + File.separator;
            String configurationPrefix = "vocabulary.plugin." + vocabularyName;
            storeHierarchy = config.getBooleanProperty(configurationPrefix + ".hierarchy.store", storeHierarchy);
            boolean storeIDs = config.getBooleanProperty(configurationPrefix + ".storeIDs", false);
            suggestHierarchy = config.getBooleanProperty(configurationPrefix + ".hierarchy.suggest", suggestHierarchy);
            preloadLevel = config.getIntProperty(configurationPrefix + ".hierarchy.preloadLevel", preloadLevel);
            String configuredDelimiter = config.getProperty(configurationPrefix + ".delimiter");
            if (configuredDelimiter != null) {
                hierarchyDelimiter = configuredDelimiter.replaceAll("(^\"|\"$)", "");
            }
            if (storeIDs) {
                valueAttribute = idAttribute;
                valueTemplate = idTemplate;
            }

            String filename = vocabulariesPath + vocabularyName + ".xml";
            if (StringUtils.isNotEmpty(locale)) {
                String localizedFilename = vocabulariesPath + vocabularyName + "_" + locale + ".xml";
                if (Paths.get(localizedFilename).toFile().exists()) {
                    filename = localizedFilename;
                }
            }
            log.info("Loading " + filename);
            vocabulary = new InputSource(filename);
        }
    }

    protected String buildString(Node node) {
        if (node.getNodeType() == Node.DOCUMENT_NODE || (
            node.getParentNode() != null &&
                node.getParentNode().getNodeType() == Node.DOCUMENT_NODE)) {
            return ("");
        } else {
            String parentValue = buildString(node.getParentNode());
            Node currentNodeValue = node.getAttributes().getNamedItem(valueAttribute);
            if (currentNodeValue != null) {
                String currentValue = currentNodeValue.getNodeValue();
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
    public Choices getMatches(String text, int start, int limit, String locale) {
        init(locale);
        log.debug("Getting matches for '" + text + "'");
        String[] textHierarchy = text.split(hierarchyDelimiter, -1);
        StringBuilder xpathExpressionBuilder = new StringBuilder();
        for (int i = 0; i < textHierarchy.length; i++) {
            xpathExpressionBuilder.append(String.format(xpathTemplate, "$var" + i));
        }
        String xpathExpression = xpathExpressionBuilder.toString();
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setXPathVariableResolver(variableName -> {
            String varName = variableName.getLocalPart();
            if (varName.startsWith("var")) {
                int index = Integer.parseInt(varName.substring(3));
                return textHierarchy[index].toLowerCase();
            }
            throw new IllegalArgumentException("Unexpected variable: " + varName);
        });
        int total;
        List<Choice> choices;
        try {
            NodeList results = (NodeList) xpath.evaluate(xpathExpression, vocabulary, XPathConstants.NODESET);
            total = results.getLength();
            choices = getChoicesFromNodeList(results, start, limit);
        } catch (XPathExpressionException e) {
            log.warn(e.getMessage(), e);
            return new Choices(true);
        }
        return new Choices(choices.toArray(new Choice[choices.size()]), start, total, Choices.CF_AMBIGUOUS,
                           total > start + limit);
    }

    @Override
    public Choices getBestMatch(String text, String locale) {
        init(locale);
        log.debug("Getting best matches for {}'", text);
        String[] textHierarchy = text.split(hierarchyDelimiter, -1);
        StringBuilder xpathExpressionBuilder = new StringBuilder();
        for (int i = 0; i < textHierarchy.length; i++) {
            xpathExpressionBuilder.append(String.format(valueTemplate, "$var" + i));
        }
        String xpathExpression = xpathExpressionBuilder.toString();
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setXPathVariableResolver(variableName -> {
            String varName = variableName.getLocalPart();
            if (varName.startsWith("var")) {
                int index = Integer.parseInt(varName.substring(3));
                return textHierarchy[index];
            }
            throw new IllegalArgumentException("Unexpected variable: " + varName);
        });
        List<Choice> choices;
        try {
            NodeList results = (NodeList) xpath.evaluate(xpathExpression, vocabulary, XPathConstants.NODESET);
            choices = getChoicesFromNodeList(results, 0, 1);
        } catch (XPathExpressionException e) {
            log.warn(e.getMessage(), e);
            return new Choices(true);
        }
        return new Choices(choices.toArray(new Choice[choices.size()]), 0, choices.size(), Choices.CF_AMBIGUOUS, false);
    }

    @Override
    public String getLabel(String key, String locale) {
        return getNodeValue(key, locale, this.suggestHierarchy);
    }

    @Override
    public String getValue(String key, String locale) {
        return getNodeValue(key, locale, this.storeHierarchy);
    }

    @Override
    public Choice getChoice(String authKey, String locale) {
        Node node;
        try {
            node = getNode(authKey, locale);
        } catch (XPathExpressionException e) {
            return null;
        }
        return createChoiceFromNode(node);
    }

    @Override
    public boolean isHierarchical() {
        init(null);
        return true;
    }

    @Override
    public Choices getTopChoices(String authorityName, int start, int limit, String locale) {
        init(locale);
        String xpathExpression = rootTemplate;
        return getChoicesByXpath(xpathExpression, start, limit);
    }

    @Override
    public Choices getChoicesByParent(String authorityName, String parentId, int start, int limit, String locale) {
        init(locale);
        String xpathExpression = String.format(idTemplateQuoted, parentId);
        return getChoicesByXpath(xpathExpression, start, limit);
    }

    @Override
    public Choice getParentChoice(String authorityName, String childId, String locale) {
        init(locale);
        try {
            String xpathExpression = String.format(idParentTemplate, childId);
            Choice choice = createChoiceFromNode(getNodeFromXPath(xpathExpression));
            return choice;
        } catch (XPathExpressionException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Integer getPreloadLevel() {
        init(null);
        return preloadLevel;
    }

    private boolean isRootElement(Node node) {
        return node != null && node.getOwnerDocument().getDocumentElement().equals(node);
    }

    private Node getNode(String key, String locale) throws XPathExpressionException {
        init(locale);
        String xpathExpression = String.format(idTemplateQuoted, key);
        Node node = getNodeFromXPath(xpathExpression);
        return node;
    }

    private Node getNodeFromXPath(String xpathExpression) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        Node node = (Node) xpath.evaluate(xpathExpression, vocabulary, XPathConstants.NODE);
        return node;
    }

    private List<Choice> getChoicesFromNodeList(NodeList results, int start, int limit) {
        List<Choice> choices = new ArrayList<>();
        for (int i = 0; i < results.getLength(); i++) {
            if (i < start) {
                continue;
            }
            if (choices.size() == limit) {
                break;
            }
            Node node = results.item(i);
            Choice choice = new Choice(getAuthority(node), getLabel(node), getValue(node),
                                       isSelectable(node));
            choice.extras = addOtherInformation(getParent(node), getNote(node), getChildren(node), getAuthority(node));
            choices.add(choice);
        }
        return choices;
    }

    private Map<String, String> addOtherInformation(String parentCurr, String noteCurr,
                                                    List<String> childrenCurr, String authorityCurr) {
        Map<String, String> extras = new HashMap<>();
        if (StringUtils.isNotBlank(parentCurr)) {
            extras.put("parent", parentCurr);
        }
        if (StringUtils.isNotBlank(noteCurr)) {
            extras.put("note", noteCurr);
        }
        if (childrenCurr.isEmpty()) {
            extras.put("hasChildren", "false");
        } else {
            extras.put("hasChildren", "true");
        }
        extras.put("id", authorityCurr);
        return extras;
    }

    private String getNodeValue(String key, String locale, boolean useHierarchy) {
        try {
            Node node = getNode(key, locale);
            if (Objects.isNull(node)) {
                return null;
            }
            if (useHierarchy) {
                return this.buildString(node);
            } else {
                return node.getAttributes().getNamedItem(valueAttribute).getNodeValue();
            }
        } catch (XPathExpressionException e) {
            return ("");
        }
    }

    private String getLabel(Node node) {
        String hierarchy = this.buildString(node);
        if (this.suggestHierarchy) {
            return hierarchy;
        } else {
            return node.getAttributes().getNamedItem("label").getNodeValue();
        }
    }

    private String getValue(Node node) {
        String hierarchy = this.buildString(node);
        if (this.storeHierarchy) {
            return hierarchy;
        } else {
            return node.getAttributes().getNamedItem(valueAttribute).getNodeValue();
        }
    }

    private String getNote(Node node) {
        NodeList childNodes = node.getChildNodes();
        for (int ci = 0; ci < childNodes.getLength(); ci++) {
            Node firstChild = childNodes.item(ci);
            if (firstChild != null && "hasNote".equals(firstChild.getNodeName())) {
                String nodeValue = firstChild.getTextContent();
                if (StringUtils.isNotBlank(nodeValue)) {
                    return nodeValue;
                }
            }
        }
        return null;
    }

    private List<String> getChildren(Node node) {
        List<String> children = new ArrayList<>();
        NodeList childNodes = node.getChildNodes();
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
        return children;
    }

    private boolean isSelectable(Node node) {
        Node selectableAttr = node.getAttributes().getNamedItem("selectable");
        if (null != selectableAttr) {
            return Boolean.parseBoolean(selectableAttr.getNodeValue());
        } else { // Default is true
            return true;
        }
    }

    private String getParent(Node node) {
        Node parentN = node.getParentNode();
        if (parentN != null) {
            parentN = parentN.getParentNode();
            if (parentN != null && !isRootElement(parentN)) {
                return buildString(parentN);
            }
        }
        return null;
    }

    private String getAuthority(Node node) {
        Node idAttr = node.getAttributes().getNamedItem("id");
        if (null != idAttr) { // 'id' is optional
            return idAttr.getNodeValue();
        } else {
            return null;
        }
    }

    private Choices getChoicesByXpath(String xpathExpression, int start, int limit) {
        List<Choice> choices = new ArrayList<>();
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            Node parentNode = (Node) xpath.evaluate(xpathExpression, vocabulary, XPathConstants.NODE);
            int count = 0;
            if (parentNode != null) {
                NodeList childNodes = (NodeList) xpath.evaluate(".//isComposedBy", parentNode, XPathConstants.NODE);
                if (null != childNodes) {
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        Node childNode = childNodes.item(i);
                        if (childNode != null && "node".equals(childNode.getNodeName())) {
                            if (count < start || choices.size() >= limit) {
                                count++;
                                continue;
                            }
                            count++;
                            choices.add(createChoiceFromNode(childNode));
                        }
                    }
                }
                return new Choices(choices.toArray(new Choice[choices.size()]), start, count,
                                   Choices.CF_AMBIGUOUS, false);
            }
        } catch (XPathExpressionException e) {
            log.warn(e.getMessage(), e);
            return new Choices(true);
        }
        return new Choices(false);
    }

    private Choice createChoiceFromNode(Node node) {
        if (node != null && !isRootElement(node)) {
            Choice choice = new Choice(getAuthority(node), getLabel(node), getValue(node),
                                       isSelectable(node));
            choice.extras = addOtherInformation(getParent(node), getNote(node), getChildren(node), getAuthority(node));
            return choice;
        }
        return null;
    }

}
