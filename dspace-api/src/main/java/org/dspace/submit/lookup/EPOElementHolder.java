/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

/***
 * Class used to order element metadata for data format. Supported format are
 * docdb, epodoc, origin.
 * 
 * @author fcadili (franceso.cadili at 4science.it)
 */
public class EPOElementHolder {
    private class ElementHolder {
        private Element element;
        private String attribute;

        private ElementHolder(Element element, String name) {
            this.element = element;
            this.attribute = name;
        }

        public String getAttributeValue() {
            return element.getAttribute(attribute);
        }

        public Element getElement() {
            return element;
        }
    }

    private Map<String, ElementHolder> holder = new HashMap<String, ElementHolder>();
    private String[] formats;

    /***
     * Add a list of elements with a data format selector to the internal map. The
     * order defined in format arrays is used when data are retrieved from the
     * internal map.
     * 
     * @param formats            The list of data format values.
     * @param elements           The elements
     * @param dataFormatSelector The attribute that store the data format values
     *                           from the elements.
     */
    public EPOElementHolder(String[] formats, List<Element> elements, String dataFormatSelector) {
        init(formats, elements, dataFormatSelector, null, null);
    }

    /***
     * Add a list of elements with a data format selector to the internal map. The
     * order defined in format arrays is used when data are retrieved from the
     * internal map.
     * 
     * @param formats            The list of data format values.
     * @param elements           The elements
     * @param dataFormatSelector The attribute that store the data format values
     * @param filterValue        The value used to filter the elements
     * @param filterSelector     The attribute used to retrieve the filter value
     *                           from the elements.
     */
    public EPOElementHolder(String[] formats, List<Element> elements, String dataFormatSelector, String filterValue,
            String filterSelector) {

        init(formats, elements, dataFormatSelector, filterValue, filterSelector);
    }

    private void init(String[] formats, List<Element> elements, String dataFormatSelector, String filterValue,
            String filterSelector) {
        this.formats = formats;
        for (Element element : elements) {
            if (StringUtils.isEmpty(filterValue) || filterValue.equals(element.getAttribute(filterSelector))) {
                add(new ElementHolder(element, dataFormatSelector));
            }
        }
    }

    private void add(ElementHolder element) {
        holder.put(element.getAttributeValue(), element);
    }

    /***
     * Retrieve the data in a ordered way using the internal formats.
     *
     * @param keys
     * @return
     */
    public Element get() {
        for (String key : formats) {
            if (holder.containsKey(key)) {
                return holder.get(key).getElement();
            }
        }

        return null;
    }

    /***
     * Retrieve the data in a ordered way using the internal formats.
     * 
     * @param keys
     * @param format
     * @return
     */
    public Element get(String format) {
        if (holder.containsKey(format)) {
            return holder.get(format).getElement();
        }

        return null;
    }
}
