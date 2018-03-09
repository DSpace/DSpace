/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * This is an interface to represent all WingElements that can be merged.
 * 
 * @author Scott Phillips
 */

public interface WingMergeableElement extends WingElement
{

    /**
     * Determine if the given SAX startElement event is equivalent to this
     * WingElement.
     * 
     * @param namespace
     *            The element's name space
     * @param localName
     *            The local, unqualified, name for this element
     * @param qName
     *            The qualified name for this element
     * @param attributes
     *            The element's attributes
     * @return True if this WingElement is equivalent to the given SAX Event.
     * @throws org.xml.sax.SAXException whenever.
     * @throws org.dspace.app.xmlui.wing.WingException whenever.
     */
    public boolean mergeEqual(String namespace, String localName, String qName,
            Attributes attributes) throws SAXException, WingException;

    /**
     * Merge the given SAX startElement event into this element's child. If this
     * SAX event matches a child element of this element then it should be
     * removed from the internal book keep of this element and returned.
     * typically this is accomplished by looping through all children elements
     * and returned the first one that returns true for the mergeEqual method.
     * 
     * @param namespace
     *            The element's name space
     * @param localName
     *            The local, unqualified, name for this element *
     * @param qName
     *            The qualified name for this element
     * @param attributes
     *            The element's attributes
     * @return The child element
     * @throws org.xml.sax.SAXException whenever.
     * @throws org.dspace.app.xmlui.wing.WingException whenever.
     */
    public WingMergeableElement mergeChild(String namespace, String localName,
            String qName, Attributes attributes) throws SAXException,
            WingException;

    /**
     * Inform this element that it is being merged with an existing element.
     * Practically this means that when this method is being transformed to SAX it
     * should assume that the element's SAX events have already been sent. In
     * this case the element would only need to transform to SAX the children of
     * this element.
     * 
     * Furthermore, if the element needs to add any attributes to the SAX
     * startElement event it may modify the attributes object passed to make
     * changes.
     * 
     * @param attributes attributes.
     * @return The attributes for this merged element
     * @throws org.xml.sax.SAXException whenever.
     * @throws org.dspace.app.xmlui.wing.WingException whenever.
     */
    public Attributes merge(Attributes attributes) throws SAXException,
            WingException;
}
