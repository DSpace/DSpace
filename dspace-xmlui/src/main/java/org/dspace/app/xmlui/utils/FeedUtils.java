/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.utils;

import java.util.Map;
import java.util.HashMap;

import org.dspace.app.util.SyndicationFeed;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility functions and data in common to all producers of syndication feeds
 * in the XMLUI -- e.g. DSpaceFeedGenerator, OpenSearchGenerator.
 *
 * Currently just implements the I18N mechanism: since the feed producer
 * is shared with the JSPUI, and that UI employs a completely different i18n
 * mechanism, the SyndicationFeed abstraction takes its own map of message
 * keys to localized values.  For the XMLUI, the "localized value" is actually
 * a string with a special sentinel prefix (defined here as I18N_PREFIX). This
 * is really just a marker; the DOM has to be post-processed later and the
 * prefixed text is replaced by a Cocoon i18n element (whose key attribute
 * is the text following the prefix).
 *
 * Note that the keys in the initial i18n message table are dictated by
 * the SyndicationFeed class.
 *
 * @see SyndicationFeed
 * @author Larry Stone
 */
public class FeedUtils
{
    public static final Map<String, String> i18nLabels = makeI18NLabels();

    /** The prefix used to differentiate i18n keys */
    private static final String I18N_PREFIX = "I18N:";

    /** Cocoon's i18n namespace */
    private static final String I18N_NAMESPACE = "http://apache.org/cocoon/i18n/2.1";

    /**
     * Returns a map of localizable labels whose values are themselves keys that are
     * de-mangled into a true i18n element for later localization.
     *
     * @return A map of mangled labels.
     */
    private static Map<String, String> makeI18NLabels()
    {
        Map<String, String> labelMap = new HashMap<String, String>();
        labelMap.put(SyndicationFeed.MSG_UNTITLED, I18N_PREFIX+"xmlui.feed.untitled");
        labelMap.put(SyndicationFeed.MSG_LOGO_TITLE, I18N_PREFIX+"xmlui.feed.logo_title");
        labelMap.put(SyndicationFeed.MSG_FEED_DESCRIPTION, I18N_PREFIX+"xmlui.feed.general_description");
        labelMap.put(SyndicationFeed.MSG_UITYPE, SyndicationFeed.UITYPE_XMLUI);
        return labelMap;
    }

    /**
     * Scan the document and replace any text nodes that begin
     * with the i18n prefix with an actual i18n element that
     * can be processed by the i18n transformer.
     *
     * @param dom the document.
     */
    public static void unmangleI18N(Document dom)
    {
        NodeList elementNodes = dom.getElementsByTagName("*");

        for (int i = 0; i < elementNodes.getLength(); i++)
        {
            NodeList textNodes = elementNodes.item(i).getChildNodes();
            for (int j = 0; j < textNodes.getLength(); j++)
            {
                Node oldNode = textNodes.item(j);
                // Check to see if the node is a text node, its value is not null, and it starts with the i18n prefix.
                if (oldNode.getNodeType() == Node.TEXT_NODE && oldNode.getNodeValue() != null && oldNode.getNodeValue().startsWith(I18N_PREFIX))
                {
                    Node parent = oldNode.getParentNode();
                    String key = oldNode.getNodeValue().substring(I18N_PREFIX.length());
                    Element newNode = dom.createElementNS(I18N_NAMESPACE, "text");
                    newNode.setAttribute("key", key);
                    newNode.setAttribute("catalogue", "default");
                    parent.replaceChild(newNode,oldNode);
                }
            }
        }
    }
}
