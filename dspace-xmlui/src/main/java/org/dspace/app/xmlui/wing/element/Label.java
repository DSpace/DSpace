/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This class represents a list's label element, they are associated with an
 * item and annotates that item with a number, a textual description of some
 * sort, or a simple bullet.
 * 
 * @author Scott Phillips
 */

public class Label extends TextContainer implements StructuralElement
{
    /** The name of the label element */
    public static final String E_LABEL = "label";

    /** The label's name */
    private final String name;

    /** Special rendering hints */
    private final String rend;

    /**
     * Construct a new label.
     * 
     * @param context
     *            (Required) The context this element is contained in
     * @param name
     *            (May be null) The label's name
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @throws WingException passed through.
     */
    protected Label(WingContext context, String name, String rend)
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
        if (this.name != null)
        {
            attributes.put(A_NAME, name);
            attributes.put(A_ID, context.generateID(E_LABEL, name));
        }
        if (this.rend != null)
        {
            attributes.put(A_RENDER, this.rend);
        }

        startElement(contentHandler, namespaces, E_LABEL, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_LABEL);
    }
}
