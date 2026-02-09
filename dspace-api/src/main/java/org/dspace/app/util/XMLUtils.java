/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.input.SAXBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Simple class to read information from small XML using DOM manipulation
 *
 * @author Andrea Bollini
 */
public class XMLUtils {

    /**
     * Default constructor
     */
    private XMLUtils() { }

    /**
     * @param dataRoot the starting node
     * @param name     the tag name of the child element to find.
     * @return the list of all DOM Element with the provided name direct child
     * of the starting node
     */
    public static List<Element> getElementList(Element dataRoot, String name) {
        NodeList list = dataRoot.getElementsByTagName(name);
        List<Element> listElements = new ArrayList<>();
        for (int i = 0; i < list.getLength(); i++) {
            Element item = (Element) list.item(i);
            if (item.getParentNode().equals(dataRoot)) {
                listElements.add(item);
            }
        }
        return listElements;
    }

    /**
     * @param dataRoot the starting node
     * @param name     the name of the sub element
     * @param attr     the attribute to get
     * @return the value of the attribute for the sub element with the specified
     * name in the starting node
     */
    public static String getElementAttribute(Element dataRoot, String name,
                                             String attr) {
        Element element = getSingleElement(dataRoot, name);
        String attrValue = null;
        if (element != null) {
            attrValue = element.getAttribute(attr);
            if (StringUtils.isNotBlank(attrValue)) {
                attrValue = attrValue.trim();
            } else {
                attrValue = null;
            }
        }
        return attrValue;
    }

    /**
     * @param dataRoot the starting node
     * @param name     the name of the sub element
     * @return the text content of the sub element with the specified name in
     * the starting node
     */
    public static String getElementValue(Element dataRoot, String name) {
        Element element = getSingleElement(dataRoot, name);
        String elementValue = null;
        if (element != null) {
            elementValue = element.getTextContent();
            if (StringUtils.isNotBlank(elementValue)) {
                elementValue = elementValue.trim();
            } else {
                elementValue = null;
            }
        }
        return elementValue;
    }

    /**
     * Return the first element child with the specified name
     *
     * @param dataRoot the starting node
     * @param name     the name of sub element to look for
     * @return the first child element or null if no present
     */
    public static Element getSingleElement(Element dataRoot, String name) {
        List<Element> nodeList = getElementList(dataRoot, name);
        Element element = null;
        if (nodeList != null && nodeList.size() > 0) {
            element = (Element) nodeList.get(0);
        }
        return element;
    }

    /**
     * @param rootElement    the starting node
     * @param subElementName the tag name of the child element to find.
     * @return a list of string including all the text contents of the sub
     * element with the specified name. If there are not sub element
     * with the supplied name the method will return null
     */
    public static List<String> getElementValueList(Element rootElement,
                                                   String subElementName) {
        if (rootElement == null) {
            return null;
        }

        List<Element> subElements = getElementList(rootElement, subElementName);
        if (subElements == null) {
            return null;
        }

        List<String> result = new ArrayList<>();
        for (Element el : subElements) {
            if (StringUtils.isNotBlank(el.getTextContent())) {
                result.add(el.getTextContent().trim());
            }
        }
        return result;
    }

    /**
     * root/subElement[]/field1, field2, fieldN
     *
     * @param rootElement    the starting node
     * @param subElementName the name of the sub element to work on
     * @param fieldsName     the names of the sub-sub-elements from which get the text
     *                       content
     * @return a list of array strings. The length of the array is equals to the
     * number of fields required. For any fields the first textual value
     * found in the sub element is used, null if no value is present
     */
    public static List<String[]> getElementValueArrayList(Element rootElement,
                                                          String subElementName, String... fieldsName) {
        if (rootElement == null) {
            return null;
        }

        List<Element> subElements = getElementList(rootElement, subElementName);
        if (subElements == null) {
            return null;
        }

        List<String[]> result = new ArrayList<>();
        for (Element el : subElements) {
            String[] tmp = new String[fieldsName.length];
            for (int idx = 0; idx < fieldsName.length; idx++) {
                tmp[idx] = XMLUtils.getElementValue(el, fieldsName[idx]);
            }
            result.add(tmp);
        }
        return result;
    }

    /**
     * Initialize and return a javax DocumentBuilderFactory with NO security
     * applied. This is intended only for internal, administrative/configuration
     * use where external entities and other dangerous features are actually
     * purposefully included.
     * The method here is tiny, but may be expanded with other features like
     * whitespace handling, and calling this method name helps to document
     * the fact that the caller knows it is trusting the XML source / factory.
     *
     * @return document builder factory to generate new builders
     * @throws ParserConfigurationException
     */
    public static DocumentBuilderFactory getTrustedDocumentBuilderFactory()
            throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        return factory;
    }

    /**
     * Initialize and return the javax DocumentBuilderFactory with some basic security
     * applied to avoid XXE attacks and other unwanted content inclusion
     * @return document builder factory to generate new builders
     * @throws ParserConfigurationException
     */
    public static DocumentBuilderFactory getDocumentBuilderFactory()
            throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // No DOCTYPE / DTDs
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        // No external general entities
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        // No external parameter entities
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        // No external DTDs
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        // Even if entities somehow get defined, they will not be expanded
        factory.setExpandEntityReferences(false);
        // Disable "XInclude" markup processing
        factory.setXIncludeAware(false);

        return factory;
    }

    /**
     * Initialize and return a javax DocumentBuilder with less security
     * applied. This is intended only for internal, administrative/configuration
     * use where external entities and other dangerous features are actually
     * purposefully included, but are only allowed from specified paths, e.g.
     * dspace.dir or some other path specified by the java caller.
     * The method here is tiny, but may be expanded with other features like
     * whitespace handling, and calling this method name helps to document
     * the fact that the caller knows it is trusting the XML source / builder
     * <p>
     * If no allowedPaths are passed, then all external entities are rejected
     *
     * @return document builder with no security features set
     * @throws ParserConfigurationException if the builder can not be configured
     */
    public static DocumentBuilder getTrustedDocumentBuilder(String... allowedPaths)
            throws ParserConfigurationException {
        DocumentBuilderFactory factory = getTrustedDocumentBuilderFactory();
        factory.setValidating(false);
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(new PathRestrictedEntityResolver(allowedPaths));
        return factory.newDocumentBuilder();
    }

    /**
     * Initialize and return the javax DocumentBuilder with some basic security applied
     * to avoid XXE attacks and other unwanted content inclusion
     * @return document builder for use in XML parsing
     * @throws ParserConfigurationException if the builder can not be configured
     */
    public static DocumentBuilder getDocumentBuilder()
            throws ParserConfigurationException {
        return getDocumentBuilderFactory().newDocumentBuilder();
    }

    /**
     * Initialize and return the SAX document builder with some basic security applied
     * to avoid XXE attacks and other unwanted content inclusion
     * @return SAX document builder for use in XML parsing
     */
    public static SAXBuilder getSAXBuilder() {
        return getSAXBuilder(false);
    }

    /**
     * Initialize and return the SAX document builder with some basic security applied
     * to avoid XXE attacks and other unwanted content inclusion
     * @param validate whether to use JDOM XSD validation
     * @return SAX document builder for use in XML parsing
     */
    public static SAXBuilder getSAXBuilder(boolean validate) {
        SAXBuilder saxBuilder = new SAXBuilder();
        if (validate) {
            saxBuilder.setValidation(true);
        }
        // No DOCTYPE / DTDs
        saxBuilder.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        // No external general entities
        saxBuilder.setFeature("http://xml.org/sax/features/external-general-entities", false);
        // No external parameter entities
        saxBuilder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        // No external DTDs
        saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        // Don't expand entities
        saxBuilder.setExpandEntities(false);

        return saxBuilder;
    }

    /**
     * Initialize and return the Java XML Input Factory with some basic security applied
     * to avoid XXE attacks and other unwanted content inclusion
     * @return XML input factory for use in XML parsing
     */
    public static XMLInputFactory getXMLInputFactory() {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        return xmlInputFactory;
    }

    /**
     * This entity resolver accepts one or more path strings in its
     * constructor and throws a SAXException if the entity systemID
     * is not within the allowed path (or a subdirectory).
     * If no parameters are passed, then this effectively disallows
     * any external entity resolution.
     */
    public static class PathRestrictedEntityResolver implements EntityResolver {
        private final List<String> allowedBasePaths;

        public PathRestrictedEntityResolver(String... allowedBasePaths) {
            this.allowedBasePaths = Arrays.asList(allowedBasePaths);
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException {

            if (systemId == null) {
                return null;
            }

            String filePath;
            if (systemId.startsWith("file://")) {
                filePath = systemId.substring(7);
            } else if (systemId.startsWith("file:")) {
                filePath = systemId.substring(5);
            } else if (!systemId.contains("://")) {
                filePath = systemId;
            } else {
                throw new SAXException("External resources not allowed: " + systemId +
                        ". Only local file paths are permitted.");
            }

            Path resolvedPath;
            try {
                resolvedPath = Paths.get(filePath).toAbsolutePath().normalize();
            } catch (Exception e) {
                throw new SAXException("Invalid path: " + systemId, e);
            }

            boolean isAllowed = false;
            for (String basePath : allowedBasePaths) {
                Path allowedPath = Paths.get(basePath).toAbsolutePath().normalize();
                if (resolvedPath.startsWith(allowedPath)) {
                    isAllowed = true;
                    break;
                }
            }

            if (!isAllowed) {
                throw new SAXException("Access denied to path: " + resolvedPath);
            }

            File file = resolvedPath.toFile();
            if (!file.exists() || !file.canRead()) {
                throw new SAXException("File not found or not readable: " + resolvedPath);
            }

            return new InputSource(new FileInputStream(file));
        }
    }


}
