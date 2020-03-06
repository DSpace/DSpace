/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

/***
 * Class used to order element metadata for data format. Supported format are
 * docdb, epodoc, origin.
 * 
 * @author fcadili (franceso.cadili at 4science.it)
 */
public class EPOElementHolder {
    private Element element;
    private String attribute;

    private static Map<String, EPOElementHolder> holder = new HashMap<String, EPOElementHolder>();

    public EPOElementHolder(Element element, String name) {
        this.element = element;
        this.attribute = name;
    }

    public String getAttributeValue() {
        return element.getAttribute(attribute);
    }

    public Element getElement() {
        return element;
    }

    public static void add(EPOElementHolder element) {
        holder.put(element.getAttributeValue(), element);
    }

    public static void clear() {
        holder.clear();
    }

    public static Element get(String[] keys) {
        for (String key : keys) {
            if (holder.containsKey(key))
                return holder.get(key).getElement();
        }

        return null;
    }
}
