/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

/**
 * A class representing a highlighted inline element of text.
 * 
 * The hi element is used for emphasis of text and occurs inside character
 * containers like "p" and "list item". It can be mixed freely with content, and
 * any content contained within the element itself will be emphasized in a
 * manner specified by the required "rend" attribute. Additionally, the "hi"
 * element is the only character container component that is recursive, allowing
 * it to contain other character components. (including other hi elements!)
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

public class Highlight extends RichTextContainer implements StructuralElement
{
    /** The name of the highlight element */
    public static final String E_HIGHLIGHT = "hi";

    /** Special rendering instructions for this highlight */
    private final String rend;

    /**
     * Construct a new highlight element.
     * 
     * @param context
     *            (Required) The context this element is contained in.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected Highlight(WingContext context, String rend) throws WingException
    {
        super(context);

        this.rend = rend;
    }

    /**
     * Translate this element and all contained elements into SAX events. The
     * events should be routed to the contentHandler found in the WingContext.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler to which SAX events
     *            should be routed.
     * @param lexicalHandler
     *            (Required) The registered lexicalHandler to which lexical
     *            events (such as CDATA, DTD, etc) should be routed.
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
        if (this.rend != null)
        {
            attributes.put(A_RENDER, this.rend);
        }

        startElement(contentHandler, namespaces, E_HIGHLIGHT, attributes);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_HIGHLIGHT);
    }
}
