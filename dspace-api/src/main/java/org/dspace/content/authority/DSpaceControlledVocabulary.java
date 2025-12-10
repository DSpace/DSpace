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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.I18nUtil;
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

    private static Logger log = LogManager.getLogger(DSpaceControlledVocabulary.class);

    protected static String xpathTemplate = "//node[contains(translate(@label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
        "'abcdefghijklmnopqrstuvwxyz'),'%s')]";
    protected static String idTemplate = "//node[@id = '%s']";
    protected static String labelTemplate = "//node[@label = '%s']";
    protected static String idParentTemplate = "//node[@id = '%s']/parent::isComposedBy/parent::node";
    protected static String rootTemplate = "/node";
    protected static String pluginNames[] = null;
    public static final String ID_SPLITTER = ":";

    protected String vocabularyName = null;
    protected Map<Locale,InputSource> vocabularies = null;
    protected Boolean suggestHierarchy = false;
    protected Boolean storeHierarchy = true;
    protected boolean storeAuthority = false;
    protected String hierarchyDelimiter = "::";
    protected Integer preloadLevel = 1;

    private ConfigurationService config;

    public DSpaceControlledVocabulary() {
        super();
        config = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    @Override
    public boolean isPublic() {
        return true;
    }

    @Override
    public boolean storeAuthorityInMetadata() {
        init();
        return storeAuthority;
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
                                                           .getProperty("dspace.dir") +
                                                           File.separator + "config" +
                                                           File.separator + "controlled-vocabularies";
            String[] xmlFiles = (new File(vocabulariesPath)).list(new xmlFilter());
            List<String> names = new ArrayList<String>();
            for (String filename : xmlFiles) {
                if (!filename.matches("(.*)_[a-z]{2}\\.xml")) {
                    names.add((new File(filename)).getName().replace(".xml", ""));
                }
            }
            pluginNames = names.toArray(new String[names.size()]);
            log.info("Got plugin names = " + Arrays.deepToString(pluginNames));
        }
    }

    private synchronized void init() {
        vocabularyName = this.getPluginInstanceName();
        log.info("Configuring " + this.getClass().getName() + ": " + vocabularyName);
        String configurationPrefix = "vocabulary.plugin." + vocabularyName;
        storeHierarchy = config.getBooleanProperty(configurationPrefix + ".hierarchy.store", storeHierarchy);
        storeAuthority = config.getBooleanProperty(configurationPrefix + ".authority.store",
                                            config.getBooleanProperty("vocabulary.plugin.authority.store", false));
        suggestHierarchy = config.getBooleanProperty(configurationPrefix + ".hierarchy.suggest", suggestHierarchy);
        preloadLevel = config.getIntProperty(configurationPrefix + ".hierarchy.preloadLevel", preloadLevel);
        String configuredDelimiter = config.getProperty(configurationPrefix + ".delimiter");
        if (configuredDelimiter != null) {
            hierarchyDelimiter = configuredDelimiter.replaceAll("(^\"|\"$)", "");
        }

        if (Objects.isNull(vocabularies)) {
            vocabularies = new HashMap<Locale, InputSource>();
            log.info("Initializing " + this.getClass().getName() + ": " + vocabularyName);
            for (Locale locale : I18nUtil.getSupportedLocales()) {
                String filename = I18nUtil.getControlledVocabularyFileName(locale, vocabularyName);
                log.info("Loading " + filename);
                vocabularies.put(locale, new InputSource(filename));
            }
        }
    }

    protected String buildString(Node node) {
        if (node.getNodeType() == Node.DOCUMENT_NODE || (
            node.getParentNode() != null &&
            node.getParentNode().getNodeType() == Node.DOCUMENT_NODE)) {
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
    public Choices getMatches(String text, int start, int limit, String locale) {
        init();
        log.debug("Getting matches for '" + text + "'");
        String xpathExpression = "";
        String[] textHierarchy = text.split(hierarchyDelimiter, -1);
        for (int i = 0; i < textHierarchy.length; i++) {
            xpathExpression += String.format(xpathTemplate, textHierarchy[i].replaceAll("'", "&apos;").toLowerCase());
        }
        XPath xpath = XPathFactory.newInstance().newXPath();
        int total = 0;
        List<Choice> choices = new ArrayList<Choice>();
        try {
            NodeList results = (NodeList) xpath.evaluate(xpathExpression, getVocabularyByLocale(locale),
                    XPathConstants.NODESET);
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
        init();
        log.debug("Getting best matches for '" + text + "'");
        String xpathExpression = "";
        String[] textHierarchy = text.split(hierarchyDelimiter, -1);
        for (int i = 0; i < textHierarchy.length; i++) {
            xpathExpression += String.format(labelTemplate, textHierarchy[i].replaceAll("'", "&apos;"));
        }
        XPath xpath = XPathFactory.newInstance().newXPath();
        List<Choice> choices = new ArrayList<Choice>();
        try {
            NodeList results = (NodeList) xpath.evaluate(xpathExpression, getVocabularyByLocale(locale),
                    XPathConstants.NODESET);
            choices = getChoicesFromNodeList(results, 0, 1);
        } catch (XPathExpressionException e) {
            log.warn(e.getMessage(), e);
            return new Choices(true);
        }
        return new Choices(choices.toArray(new Choice[choices.size()]), 0, choices.size(), Choices.CF_AMBIGUOUS, false);
    }

    @Override
    public String getLabel(String key, String locale) {
        init();
        return getNodeLabel(key, this.suggestHierarchy, locale);
    }

    @Override
    public String getValue(String key, String locale) {
        init();
        return getNodeLabel(key, this.storeHierarchy, locale);
    }

    @Override
    public Choice getChoice(String authKey, String locale) {
        init();
        //FIXME hack to deal with an improper use on the angular side of the node id (otherinformation.id) to
        // build a vocabulary entry details ID
        if (!StringUtils.startsWith(authKey, vocabularyName)) {
            authKey = vocabularyName + DSpaceControlledVocabulary.ID_SPLITTER + authKey;
        }
        String nodeId = getNodeIdFromAuthorityKey(authKey);
        Node node;
        try {
            node = getNode(nodeId, locale);
        } catch (XPathExpressionException e) {
            return null;
        }
        return createChoiceFromNode(vocabularyName, node);
    }

    @Override
    public boolean isHierarchical() {
        init();
        return true;
    }

    @Override
    public Choices getTopChoices(String authorityName, int start, int limit, String locale) {
        init();
        String xpathExpression = rootTemplate;
        return getChoicesByXpath(authorityName, xpathExpression, start, limit, locale);
    }

    @Override
    public Choices getChoicesByParent(String authorityName, String parentAuthKey, int start, int limit, String locale) {
        init();
        String parentId = getNodeIdFromAuthorityKey(parentAuthKey);
        String xpathExpression = String.format(idTemplate, parentId);
        return getChoicesByXpath(authorityName, xpathExpression, start, limit, locale);
    }

    @Override
    public Choice getParentChoice(String authorityName, String childAuthKey, String locale) {
        init();
        try {
            String childId = getNodeIdFromAuthorityKey(childAuthKey);
            String xpathExpression = String.format(idParentTemplate, childId);
            Choice choice = createChoiceFromNode(authorityName, getNodeFromXPath(xpathExpression, locale));
            return choice;
        } catch (XPathExpressionException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Integer getPreloadLevel() {
        init();
        return preloadLevel;
    }

    private String getNodeIdFromAuthorityKey(String authKey) {
        if (StringUtils.isNotBlank(authKey)) {
            String[] split = authKey.split(ID_SPLITTER, 2);
            if (split.length == 2) {
                return split[1];
            }
        }
        return null;
    }

    private boolean isRootElement(Node node) {
        if (node != null && node.getOwnerDocument().getDocumentElement().equals(node)) {
            return true;
        }
        return false;
    }

    private Node getNode(String nodeId, String locale) throws XPathExpressionException {
        init();
        String xpathExpression = String.format(idTemplate, nodeId);
        Node node = getNodeFromXPath(xpathExpression, locale);
        return node;
    }

    private Node getNodeFromXPath(String xpathExpression, String locale) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        Node node = (Node) xpath.evaluate(xpathExpression, getVocabularyByLocale(locale), XPathConstants.NODE);
        return node;
    }

    private List<Choice> getChoicesFromNodeList(NodeList results, int start, int limit) {
        List<Choice> choices = new ArrayList<Choice>();
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
            choice.extras = addOtherInformation(getParent(node), getNote(node), getChildren(node), getNodeId(node));
            choices.add(choice);
        }
        return choices;
    }

    private Map<String, String> addOtherInformation(String parentCurr, String noteCurr,
            List<String> childrenCurr, String nodeId) {
        Map<String, String> extras = new HashMap<String, String>();
        if (StringUtils.isNotBlank(parentCurr)) {
            extras.put("parent", parentCurr);
        }
        if (StringUtils.isNotBlank(noteCurr)) {
            extras.put("note", noteCurr);
        }
        if (childrenCurr.size() > 0) {
            extras.put("hasChildren", "true");
        } else {
            extras.put("hasChildren", "false");
        }
        extras.put("id", nodeId);
        return extras;
    }

    private String getNodeLabel(String key, boolean useHierarchy, String locale) {
        try {
            String nodeId = getNodeIdFromAuthorityKey(key);
            Node node = getNode(nodeId, locale);
            if (Objects.isNull(node)) {
                return null;
            }
            if (useHierarchy) {
                return this.buildString(node);
            } else {
                return node.getAttributes().getNamedItem("label").getNodeValue();
            }
        } catch (XPathExpressionException e) {
            return ("");
        }
    }

    private String getLabel(Node node) {
        if (this.suggestHierarchy) {
            String hierarchy = this.buildString(node);
            return hierarchy;
        } else {
            return node.getAttributes().getNamedItem("label").getNodeValue();
        }
    }

    private String getValue(Node node) {
        if (this.storeHierarchy) {
            String hierarchy = this.buildString(node);
            return hierarchy;
        } else {
            return node.getAttributes().getNamedItem("label").getNodeValue();
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
        List<String> children = new ArrayList<String>();
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
            return Boolean.valueOf(selectableAttr.getNodeValue());
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
            return getPluginInstanceName() + ID_SPLITTER + idAttr.getNodeValue();
        } else {
            return null;
        }
    }

    private String getNodeId(Node node) {
        Node idAttr = node.getAttributes().getNamedItem("id");
        if (null != idAttr) { // 'id' is optional
            return idAttr.getNodeValue();
        } else {
            return null;
        }
    }

    private Choices getChoicesByXpath(String authorityName, String xpathExpression, int start, int limit,
            String locale) {
        List<Choice> choices = new ArrayList<Choice>();
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            Node parentNode = (Node) xpath.evaluate(xpathExpression, getVocabularyByLocale(locale),
                    XPathConstants.NODE);
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
                            choices.add(createChoiceFromNode(authorityName, childNode));
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

    private Choice createChoiceFromNode(String authorityName, Node node) {
        if (node != null && !isRootElement(node)) {
            Choice choice = new Choice(
                    authorityName,
                    getAuthority(node),
                    getLabel(node),
                    getValue(node),
                    isSelectable(node)
            );
            choice.extras = addOtherInformation(getParent(node), getNote(node), getChildren(node), getNodeId(node));
            return choice;
        }
        return null;
    }

    private InputSource getVocabularyByLocale(String locale) {
        Locale currentLocale = I18nUtil.getSupportedLocale(locale);
        return vocabularies.get(currentLocale);
    }
}
