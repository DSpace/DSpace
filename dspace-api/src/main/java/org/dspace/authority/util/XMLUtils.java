/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class XMLUtils {

    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(XMLUtils.class);

    /**
     * Default constructor
     */
    private XMLUtils() { }

    /**
     * @param xml             The starting context (a Node or a Document, for example).
     * @param singleNodeXPath xpath
     * @return node.getTextContent() on the node that matches singleNodeXPath
     * null if nothing matches the NodeListXPath
     * @throws XPathExpressionException if xpath error
     */
    public static String getTextContent(Node xml, String singleNodeXPath) throws XPathExpressionException {
        String text = null;
        Node node = getNode(xml, singleNodeXPath);
        if (node != null) {
            text = node.getTextContent();
        }

        return text;
    }

    /**
     * @param xml           The starting context (a Node or a Document, for example).
     * @param nodeListXPath xpath
     * @return A Node matches the NodeListXPath
     * null if nothing matches the NodeListXPath
     * @throws XPathExpressionException if xpath error
     */
    public static Node getNode(Node xml, String nodeListXPath) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        return (Node) xPath.compile(nodeListXPath).evaluate(xml, XPathConstants.NODE);
    }

    /**
     * @param xml           The starting context (a Node or a Document, for example).
     * @param nodeListXPath xpath
     * @return A NodeList containing the nodes that match the NodeListXPath
     * null if nothing matches the NodeListXPath
     * @throws XPathExpressionException if xpath error
     */
    public static NodeList getNodeList(Node xml, String nodeListXPath) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        return (NodeList) xPath.compile(nodeListXPath).evaluate(xml, XPathConstants.NODESET);
    }

    public static Iterator<Node> getNodeListIterator(Node xml, String NodeListXPath) throws XPathExpressionException {
        return getNodeListIterator(getNodeList(xml, NodeListXPath));
    }

    /**
     * Creates an iterator for all direct child nodes within a given NodeList
     * that are element nodes:
     * node.getNodeType() == Node.ELEMENT_NODE
     * node instanceof Element
     *
     * @param nodeList NodeList
     * @return iterator over nodes
     */
    public static Iterator<Node> getNodeListIterator(final NodeList nodeList) {
        return new Iterator<Node>() {
            private Iterator<Node> nodeIterator;
            private Node lastNode;

            {
                ArrayList<Node> nodes = new ArrayList<Node>();
                if (nodeList != null) {
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        Node node = nodeList.item(i);
                        //if (node.getNodeType() != Node.TEXT_NODE) {
                        if (node.getNodeType() == Node.ELEMENT_NODE && node instanceof Element) {
                            nodes.add(node);
                        }
                    }
                }
                nodeIterator = nodes.iterator();
            }

            @Override
            public boolean hasNext() {
                return nodeIterator.hasNext();
            }

            @Override
            public Node next() {
                lastNode = nodeIterator.next();
                return lastNode;
            }

            @Override
            public void remove() {
                nodeIterator.remove();
                //                lastNode.drop();
            }
        };
    }

    public static Document convertStreamToXML(InputStream is) {
        Document result = null;
        if (is != null) {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder builder = domFactory.newDocumentBuilder();
                result = builder.parse(is);
            } catch (ParserConfigurationException e) {
                log.error("Error", e);
            } catch (SAXException e) {
                log.error("Error", e);
            } catch (IOException e) {
                log.error("Error", e);
            }
        }
        return result;

    }
}
