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
 * This class represents a head for either a table, division, or list.
 * 
 * @author Scott Phillips
 */

public class Head extends TextContainer implements StructuralElement
{
    /** The name of the head element */
    public static final String E_HEAD = "head";

    /** The head's name */
    private final String name;

    /**
     * Construct a new head.
     * 
     * @param context
     *            (Required) The context this element is contained in
     * @param name
     *            (May be null) a local identifier used to differentiate the
     *            element from its siblings.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected Head(WingContext context, String name) throws WingException
    {
        super(context);
        this.name = name;
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
            attributes.put(A_ID, context.generateID(E_HEAD, name));
        }

        startElement(contentHandler, namespaces, E_HEAD, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_HEAD);
    }
}
