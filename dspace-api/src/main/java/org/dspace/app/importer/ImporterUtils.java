package org.dspace.app.importer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ImporterUtils
{
    public static String normalizeUpperCase(String delimitatorRegEx, String value)
    {
        if (value == null || value.isEmpty()) return value;
        Pattern pattern = Pattern.compile("(.*)("+delimitatorRegEx+")(.)(.*)");
        String precedente= value.toLowerCase();
        
        Matcher matcher = pattern.matcher(precedente);
        StringBuffer sb = new StringBuffer(value.length());
        
        while (matcher.matches())
        {
            String del = matcher.group(2);
            precedente = matcher.group(1);
            
            String residuo = matcher.group(4);
            sb.insert(0, residuo);
            sb.insert(0, matcher.group(3).toUpperCase());
            sb.insert(0, del);
            matcher = pattern.matcher(precedente);
        }
        sb.insert(0,precedente);
        sb.replace(0, 1, sb.substring(0, 1).toUpperCase());
        return sb.toString();
    }

    public static List<Element> getElementList(Element dataRoot, String name)
    {
        NodeList list = dataRoot.getElementsByTagName(name);
        List<Element> listElements = new ArrayList<Element>();
        for (int i = 0; i < list.getLength(); i++)
        {
            listElements.add((Element) list.item(i));
        }
        return listElements;
    }

    public static String getElementAttribute(Element dataRoot, String name,
            String attr)
    {
        NodeList nodeList = dataRoot.getElementsByTagName(name);
        Element element = null;
        if (nodeList != null && nodeList.getLength() > 0)
        {
            element = (Element) nodeList.item(0);
        }
        
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

    public static String getElementValue(Element dataRoot, String name)
    {
        NodeList nodeList = dataRoot.getElementsByTagName(name);
        Element element = null;
        if (nodeList != null && nodeList.getLength() > 0)
        {
            element = (Element) nodeList.item(0);
        }
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
     * Restituisce il primo sottoelemeto di xmlRoot trovato con il nome
     * specificato
     * 
     * @param xmlRoot
     *            l'elemento in cui cercare (NOT null)
     * @param name
     *            il nome del sottoelemento da restituire
     * @return il primo sottoelemento trovato o null se non � presente
     */
    public static Element getSingleElement(Element xmlRoot, String name)
    {
        NodeList nodeList = xmlRoot.getElementsByTagName(name);
        Element element = null;
        if (nodeList != null && nodeList.getLength() > 0)
        {
            element = (Element) nodeList.item(0);
        }
        return element;
    }

    /**
     * 
     * @param rootElement
     *            l'elemento in cui cercare
     * @param subElementName
     *            il nome del sottoelemento di cui estrarre il valore
     * @return una lista di stringhe contenente tutti i valori del sottoelemento
     *         cercato. Null se non sono presenti sottoelementi o l'elemento
     *         radice � null
     */
    public static List<String> getElementValueList(
            Element rootElement, String subElementName)
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
     *            l'elemnto radice
     * @param subElementName
     *            il nome del sottoelemento di cui si vuole processare il
     *            contenuto
     * @param fieldsName
     *            uno o pi� sotto-sotto-elementi xml di cui si vuole estrarre il
     *            valore testuale
     * @return una lista di array di stringhe, la dimensione dell'array �
     *         determinata dal numero di fields richiesti. Per ogni field viene
     *         inserita nella corrispondente posizione dell'array il valore
     *         testuale del primo soto-sotto-elemento xml trovato, null se non �
     *         presente
     */
    public static List<String[]> getElementValueArrayList(
            Element rootElement, String subElementName, String... fieldsName)
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
                tmp[idx] = ImporterUtils.getElementValue(el, fieldsName[idx]);
            }
            result.add(tmp);
        }
        return result;
    }
}
