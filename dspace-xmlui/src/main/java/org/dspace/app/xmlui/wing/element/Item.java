/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

/**
 * A class representing a list item.
 * 
 * The item element in a list serves a dual purpose. It can contain other lists,
 * allowing for hierarchies and recursive lists. Alternatively it can serve as a
 * character container to display textual data, possibly enhanced with
 * hyperlinks, emphasized blocks of text, images and form fields. An item cannot
 * be both a character container and contain a list.
 * 
 * @author Scott Phillips
 */

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

public class Item extends RichTextContainer implements StructuralElement
{
    /** The name of the item element */
    public static final String E_ITEM = "item";

    /** the item's name */
    private final String name;

    /** Special rendering hints for this item */
    private final String rend;

    /**
     * Construct a new item.
     * 
     * @param context
     *            (Required) The context this element is contained in
     * @param name
     *            (May be null) a local identifier used to differentiate the
     *            element from its siblings.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected Item(WingContext context, String name, String rend)
            throws WingException
    {
        super(context);
        this.name = name;
        this.rend = rend;
    }

    
    /**
     * Translate this element and all contained elements into SAX events. The
     * events should be routed to the contentHandler found in the WingContext.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param lexicalHandler
     *            (Required) The registered lexicalHandler where lexical 
     *            events (such as CDATA, DTD, etc) should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     * @throws org.xml.sax.SAXException passed through.
     */
    @Override
    public void toSAX(ContentHandler contentHandler, LexicalHandler lexicalHandler, 
            NamespaceSupport namespaces) throws SAXException
    {
        AttributeMap attributes = new AttributeMap();
        if (name != null)
        {
            attributes.put(A_NAME, name);
        }
        if (name != null)
        {
            attributes.put(A_ID, context.generateID(E_ITEM, name));
        }
        if (rend != null)
        {
            attributes.put(A_RENDER, rend);
        }

        startElement(contentHandler, namespaces, E_ITEM, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_ITEM);
    }
}
