/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Simple class to read information from small XML using DOM manipulation
 * 
 * @author Andrea Bollini
 * 
 */
public class XMLUtils
{
    /**
     * 
     * @param dataRoot
     *            the starting node
     * @param name
     *            the name of the subelement to find
     * @return the list of all DOM Element with the provided name direct child
     *         of the starting node
     */
    public static List<Element> getElementList(Element dataRoot, String name)
    {
        NodeList list = dataRoot.getElementsByTagName(name);
        List<Element> listElements = new ArrayList<Element>();
        for (int i = 0; i < list.getLength(); i++)
        {
            Element item = (Element) list.item(i);
            if (item.getParentNode().equals(dataRoot))
            {
                listElements.add(item);
            }
        }
        return listElements;
    }

    /**
     * 
     * @param dataRoot
     *            the starting node
     * @param name
     *            the name of the sub element
     * @param attr
     *            the attribute to get
     * @return the value of the attribute for the sub element with the specified
     *         name in the starting node
     */
    public static String getElementAttribute(Element dataRoot, String name,
            String attr)
    {
        Element element = getSingleElement(dataRoot, name);
        String attrValue = null;
        if (element != null)
        {
            attrValue = element.getAttribute(attr);
            if (StringUtils.isNotBlank(attrValue))
            {
                attrValue = attrValue.trim();
            }
            else
                attrValue = null;
        }
        return attrValue;
    }

    /**
     * 
     * @param dataRoot
     *            the starting node
     * @param name
     *            the name of the sub element
     * @return the text content of the sub element with the specified name in
     *         the starting node
     */
    public static String getElementValue(Element dataRoot, String name)
    {
        Element element = getSingleElement(dataRoot, name);
        String elementValue = null;
        if (element != null)
        {
            elementValue = element.getTextContent();
            if (StringUtils.isNotBlank(elementValue))
            {
                elementValue = elementValue.trim();
            }
            else
                elementValue = null;
        }
        return elementValue;
    }

    /**
     * Return the first element child with the specified name
     * 
     * @param dataRoot
     *            the starting node
     * @param name
     *            the name of sub element to look for
     * @return the first child element or null if no present
     */
    public static Element getSingleElement(Element dataRoot, String name)
    {
        List<Element> nodeList = getElementList(dataRoot, name);
        Element element = null;
        if (nodeList != null && nodeList.size() > 0)
        {
            element = (Element) nodeList.get(0);
        }
        return element;
    }

    /**
     * 
     * @param rootElement
     *            the starting node
     * @param subElementName
     *            the name of the subelement to find
     * @return a list of string including all the text contents of the sub
     *         element with the specified name. If there are not sub element
     *         with the supplied name the method will return null
     */
    public static List<String> getElementValueList(Element rootElement,
            String subElementName)
    {
        if (rootElement == null)
            return null;

        List<Element> subElements = getElementList(rootElement, subElementName);
        if (subElements == null)
            return null;

        List<String> result = new LinkedList<String>();
        for (Element el : subElements)
        {
            if (StringUtils.isNotBlank(el.getTextContent()))
            {
                result.add(el.getTextContent().trim());
            }
        }
        return result;
    }

    /**
     * root/subElement[]/field1, field2, fieldN
     * 
     * @param rootElement
     *            the starting node
     * @param subElementName
     *            the name of the sub element to work on
     * @param fieldsName
     *            the names of the sub-sub-elements from which get the text
     *            content
     * @return a list of array strings. The length of the array is equals to the
     *         number of fields required. For any fields the first textual value
     *         found in the sub element is used, null if no value is present
     */
    public static List<String[]> getElementValueArrayList(Element rootElement,
            String subElementName, String... fieldsName)
    {
        if (rootElement == null)
            return null;

        List<Element> subElements = getElementList(rootElement, subElementName);
        if (subElements == null)
            return null;

        List<String[]> result = new LinkedList<String[]>();
        for (Element el : subElements)
        {
            String[] tmp = new String[fieldsName.length];
            for (int idx = 0; idx < fieldsName.length; idx++)
            {
                tmp[idx] = XMLUtils.getElementValue(el, fieldsName[idx]);
            }
            result.add(tmp);
        }
        return result;
    }
}