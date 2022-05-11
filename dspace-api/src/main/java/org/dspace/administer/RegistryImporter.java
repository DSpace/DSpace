/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Richard Jones
 *
 * This class provides the tools that registry importers might need to
 * use.  Basically some utility methods.  And actually, although it says
 * I am the author, really I ripped these methods off from other
 * classes
 */
public class RegistryImporter {

    /**
     * Default constructor
     */
    private RegistryImporter() { }

    /**
     * Load in the XML from file.
     *
     * @param filename the filename to load from
     * @return the DOM representation of the XML file
     * @throws IOException                  if IO error
     * @throws ParserConfigurationException if configuration parse error
     * @throws SAXException                 if XML parse error
     */
    public static Document loadXML(String filename)
        throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance()
                                                        .newDocumentBuilder();

        Document document = builder.parse(new File(filename));

        return document;
    }

    /**
     * Get the CDATA of a particular element. For example, if the XML document
     * contains:
     * <P>
     * <code>
     * &lt;foo&gt;&lt;mimetype&gt;application/pdf&lt;/mimetype&gt;&lt;/foo&gt;
     * </code>
     * passing this the <code>foo</code> node and <code>mimetype</code> will
     * return <code>application/pdf</code>.
     * </P>
     * Why this isn't a core part of the XML API I do not know...
     *
     * @param parentElement the element, whose child element you want the CDATA from
     * @param childName     the name of the element you want the CDATA from
     * @return the CDATA as a <code>String</code>
     * @throws TransformerException if error
     */
    public static String getElementData(Node parentElement, String childName)
        throws XPathExpressionException {
        // Grab the child node
        XPath xPath = XPathFactory.newInstance().newXPath();
        Node childNode = (Node) xPath.compile(childName).evaluate(parentElement, XPathConstants.NODE);

        if (childNode == null) {
            // No child node, so no values
            return null;
        }

        // Get the #text
        Node dataNode = childNode.getFirstChild();

        if (dataNode == null) {
            return null;
        }

        // Get the data
        String value = dataNode.getNodeValue().trim();

        return value;
    }

    /**
     * Get repeated CDATA for a particular element. For example, if the XML
     * document contains:
     * <P>
     * <code>
     * &lt;foo&gt;
     * &lt;bar&gt;val1&lt;/bar&gt;
     * &lt;bar&gt;val2&lt;/bar&gt;
     * &lt;/foo&gt;
     * </code>
     * passing this the <code>foo</code> node and <code>bar</code> will
     * return <code>val1</code> and <code>val2</code>.
     * </P>
     * Why this also isn't a core part of the XML API I do not know...
     *
     * @param parentElement the element, whose child element you want the CDATA from
     * @param childName     the name of the element you want the CDATA from
     * @return the CDATA as a <code>String</code>
     * @throws TransformerException if error
     */
    public static String[] getRepeatedElementData(Node parentElement,
                                                  String childName) throws XPathExpressionException {
        // Grab the child node
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList childNodes = (NodeList) xPath.compile(childName).evaluate(parentElement, XPathConstants.NODESET);

        String[] data = new String[childNodes.getLength()];

        for (int i = 0; i < childNodes.getLength(); i++) {
            // Get the #text node
            Node dataNode = childNodes.item(i).getFirstChild();

            // Get the data
            data[i] = dataNode.getNodeValue().trim();
        }

        return data;
    }
}
